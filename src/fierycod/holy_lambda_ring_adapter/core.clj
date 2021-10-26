(ns fierycod.holy-lambda-ring-adapter.core
  (:import
   [java.io InputStream File ByteArrayInputStream]
   [java.util Base64]
   [clojure.lang ISeq IPersistentMap])
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(defprotocol ResponseBody
  (adapt [body] "Adapts the RingResponse body to valid AWS Lambda body"))

(extend-protocol ResponseBody
  ByteArrayInputStream
  (adapt [^ByteArrayInputStream body]
    {:body (slurp body)
     :encoded? false})

  InputStream
  (adapt [^InputStream body]
    {:body (slurp body)
     :encoded? false})

  File
  (adapt [^File body]
    {:body (slurp body)
     :encoded? true})

  ISeq
  (adapt [^ISeq body]
    {:body (s/join "\n" (map adapt body))
     :encoded? false})

  IPersistentMap
  (adapt [^IPersistentMap body]
    {:body body
     :encoded? false})

  Object
  (adapt [^Object body]
    {:body (str body)
     :encoded? false})

  nil
  (adapt [body]
    {:body body
     :encoded? false}))

(defn hl-request->ring-request
  [{:keys [event ctx]}]
  (let [request-ctx (event :requestContext)
        http        (request-ctx :http)
        headers     (event :headers)
        base64?     (event :isBase64Encoded)]
    {:server-port    (some-> (get headers "x-forwarded-port") (Integer/parseInt))
     :body           (when-let [^String body (event :body)]
                       (io/input-stream
                        (if-not base64?
                          (.getBytes body)
                          (.decode (Base64/getDecoder) body))))
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
  [response]
  (let [^ResponseBody body      (:body response)
        {:keys [body encoded?]} (adapt body)]
    {:statusCode      (:status response)
     :body            body
     :isBase64Encoded encoded?
     :headers         (:headers response)}))

(defn wrap-hl-req-res-model
  "Middleware that converts Ring Request/Response model to Holy Lambda (AWS Lambda) Request/Response model.
   Ideal for running regular ring applications on AWS Lambda.

   Middleware supports both `sync/async` ring handlers.

   **Example**

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

   (def HttpApiProxyGateway (hlra/wrap-hl-req-res-model ring-handler))

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
