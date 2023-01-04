(ns fierycod.holy-lambda-ring-adapter.core
  (:require
   [clojure.string :as s]
   [fierycod.holy-lambda-ring-adapter.codec :as codec]
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
  (let [request-ctx (get event :requestContext)
        http        (get request-ctx :http)
        headers     (persistent! (reduce (fn [acc [k v]]
                                           (if (keyword? k)
                                             (assoc! acc (.toLowerCase (name k)) v)
                                             (assoc! acc k v)))
                                         (transient {})
                                         (get event :headers)))
        base64?     (get event :isBase64Encoded)]
    (when-not request-ctx
      (throw (ex-info "Incorrect shape of AWS event. The adapter is compatible with following integrations: HttpApi and RestApi on AWS Api Gateway service. If you're testing locally make sure the event shape is valid e.g. use `sam local start-api` instead of `sam local invoke`." {:ctx :hl-ring-adapter})))

    {:server-port    (some-> (get headers "x-forwarded-port") (Integer/parseInt))
     :body           (impl/to-ring-request-body (:body event) base64?)
     :server-name    (or (get http :sourceIp)
                         (get-in request-ctx [:identity :sourceIp]))
     :remote-addr    (or (get http :sourceIp)
                         (get-in request-ctx [:identity :sourceIp]))
     :uri            (or (get http :path)
                         (get event :path))
     :query-string   (or (get event :rawQueryString)
                         (some-> (get event :queryStringParameters)
                                 codec/form-encode))
     :scheme         (some-> (get headers "x-forwarded-proto" "http") keyword)
     :request-method (some-> (or (get http :method)
                                 (get request-ctx :httpMethod))
                             s/lower-case
                             keyword)
     :protocol       (or (get http :protocol)
                         (get request-ctx :protocol))
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
  (let [^impl/RingResponseBody body                (:body response)
        {:keys [body encoded?]}                    (impl/to-hl-response-body body)
        [single-value-headers multi-value-headers] ((juxt remove filter)
                                                    (comp coll? second)
                                                    (:headers response))]
    (cond-> {:statusCode      (:status response)
             :body            body
             :isBase64Encoded encoded?}
      (seq single-value-headers) (assoc :headers (into {} single-value-headers))
      (seq multi-value-headers) (assoc :multiValueHeaders (into {} multi-value-headers)))))

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
     (try
       (handler (hl-request->ring-request!! request)
                (fn [response]
                  (try
                    (respond (ring-response->hl-response response))
                    (catch Exception ex
                      (raise ex))))
                raise)
       (catch Exception ex
         (raise ex))))))

(def ^:deprecated wrap-hl-req-res-model
  "DEPRECATED. Subject to remove in 0.5.0.
  Use `ring<->hl-middleware` instead!"
  ring<->hl-middleware)
