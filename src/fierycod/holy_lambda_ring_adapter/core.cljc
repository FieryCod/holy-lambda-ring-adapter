(ns fierycod.holy-lambda-ring-adapter.core
  (:require
   [clojure.string :as s]
   [fierycod.holy-lambda-ring-adapter.impl :as impl]))

#?(:bb nil
   :clj (set! *warn-on-reflection* true))

(defn hl-request->ring-request
  "Transforms valid Holy Lambda request to compatible Ring request

  **Examples**

  ```clojure
  (ns core
   (:require
    [fierycod.holy-lambda-ring-adapter.core :as hlra]
    [fierycod.holy-lambda.core :as h])

  (defn ring-handler
    [response]
    {:status 200
     :headers {}
     :body \"Hello World\"})

  (defn HttpApiProxyGateway
    [request]
    (hlra/ring-response->hl-response (ring-handler (hlra/hl-request->ring-request request))))

  (h/entrypoint [#'HttpApiProxyGateway])
  ```"
  [{:keys [event ctx]}]
  (let [request-ctx (event :requestContext)
        http        (request-ctx :http)
        headers     (event :headers)
        base64?     (event :isBase64Encoded)]
    {:server-port    #?(:clj (some-> (get headers "x-forwarded-port") (Integer/parseInt))
                        :default nil)
     :body           (impl/to-ring-request-body (:body event) base64?)
     :server-name    (get http :sourceIp)
     :remote-addr    (get http :sourceIp)
     :uri            (get http :path)
     :query-string   (get event :rawQueryString)
     :scheme         (keyword (get headers "x-forwarded-proto"))
     :request-method (keyword (s/lower-case (get http :method)))
     :protocol       (get http :protocol)
     :headers        headers
     :lambda         {:ctx   ctx
                      :event event}}))

(defn- hl-request->ring-request!!
  [request]
  (assert (and (contains? request :event) (contains? request :ctx))
          "Incorrect Holy Lambda Request/Response model. Incorrect middleware position?")
  (hl-request->ring-request request))

(defn ring-response->hl-response
  "Transforms valid Ring response to Holy Lambda compatible response

  **Examples**

  ```clojure
  (ns core
   (:require
    [fierycod.holy-lambda-ring-adapter.core :as hlra]
    [fierycod.holy-lambda.core :as h])

  (defn ring-handler
    [response]
    {:status 200
     :headers {}
     :body \"Hello World\"})

  (defn HttpApiProxyGateway
    [request]
    (hlra/ring-response->hl-response (ring-handler (hlra/hl-request->ring-request request))))

  (h/entrypoint [#'HttpApiProxyGateway])
  ```"
  [response]
  (let [^RingResponseBody body  (:body response)
        {:keys [body encoded?]} (impl/to-hl-response-body body)]
    {:statusCode      (:status response)
     :body            body
     :isBase64Encoded encoded?
     :headers         (:headers response)}))

(defn ring<->hl-middleware
  "Middleware that converts Ring Request/Response model to Holy Lambda (AWS Lambda) Request/Response model.
  Ideal for running regular ring applications on AWS Lambda.

  Middleware supports both `sync/async` ring handlers.

  **Examples**

  1. With plain Ring:

  ```clojure
  (ns core
   (:require
    [fierycod.holy-lambda-ring-adapter.core :as hlra]
    [fierycod.holy-lambda.core :as h])

  (defn ring-handler
    [request]
    {:status 200
     :headers {}
     :body \"Hello World\"}

  (def HttpApiProxyGateway (hlra/ring<->hl-middleware ring-handler))

  (h/entrypoint [#'HttpApiProxyGateway])
  ```

  2. With muuntaja:

  ```clojure
  (ns core
   (:require
    [fierycod.holy-lambda-ring-adapter.core :as hlra]
    [fierycod.holy-lambda.core :as h])

  (def muuntaja-ring-handler
    (ring/ring-handler
      (ring/router
        routes
        {:data {:muuntaja   instance
                :coercion   coerction
                :middleware middlewares}})))

  (def HttpApiProxyGateway (hlra/ring<->hl-middleware muuntaja-ring-handler))

  (h/entrypoint [#'HttpApiProxyGateway])
  ```"
  [handler]
  (fn
    ([request]
     (ring-response->hl-response (handler (hl-request->ring-request!! request))))
    ([request respond raise]
     (handler (hl-request->ring-request!! request)
              (fn [response] (respond (ring-response->hl-response response)))
              raise))))
