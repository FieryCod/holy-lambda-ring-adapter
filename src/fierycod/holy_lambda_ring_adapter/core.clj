(ns fierycod.holy-lambda-ring-adapter.core
  (:import
   [java.io InputStream File ByteArrayInputStream]
   [java.util Base64 Base64$Decoder Base64$Encoder]
   [java.net URL]
   [java.nio.file Files]
   [clojure.lang ISeq IPersistentMap PersistentVector IPersistentSet])
  (:require
   [ring.util.response :as resp]
   [clojure.string :as s]
   [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

;; See https://github.com/ring-clojure/ring/pull/447/files
(defmethod resp/resource-data :resource
  [^URL url]
  ;; GraalVM resource scheme
  (let [resource (.openConnection url)]
    {:content        (.getInputStream resource)
     :content-length (#'ring.util.response/connection-content-length resource)
     :last-modified  (#'ring.util.response/connection-last-modified resource)}))

(def base64-decoder (delay (Base64/getDecoder)))
(def base64-encoder (delay (Base64/getEncoder)))

(defprotocol ^:private RingResponseBody
  (to-hl-response-body [body] "Adapts the RingResponseBody to valid HLResponseBody"))

(extend-protocol RingResponseBody
  ByteArrayInputStream
  (to-hl-response-body [^ByteArrayInputStream body]
    {:body     (.readAllBytes body)
     :encoded? false})

  InputStream
  (to-hl-response-body [^InputStream body]
    {:body     (.readAllBytes body)
     :encoded? false})

  File
  (to-hl-response-body [^File body]
    {:body     (.encode ^Base64$Encoder @base64-encoder ^bytes (Files/readAllBytes (.toPath body)))
     :encoded? true})

  String
  (to-hl-response-body [^String body]
    {:body    (.getBytes body)
     :encoded? false})

  URL
  (to-hl-response-body [^URL body]
    {:body     (.encode ^Base64$Encoder @base64-encoder ^bytes (.readAllBytes (.getInputStream (.openConnection body))))
     :encoded? true})

  IPersistentSet
  (to-hl-response-body [^IPersistentMap body]
    {:body     body
     :encoded? false})

  PersistentVector
  (to-hl-response-body [^IPersistentMap body]
    {:body     body
     :encoded? false})

  IPersistentMap
  (to-hl-response-body [^IPersistentMap body]
    {:body     body
     :encoded? false})

  ISeq
  (to-hl-response-body [^ISeq body]
    {:body     (.getBytes (s/join "\n" (map str body)))
     :encoded? false})

  Object
  (to-hl-response-body [^Object body]
    {:body     (.getBytes (str body))
     :encoded? false})

  nil
  (to-hl-response-body [_]
    {:body     nil
     :encoded? false}))

(defprotocol ^:private HLRequestBody
  (to-ring-request-body [body base64?] "Adapts the HLRequestBody to valid RingRequestBody"))

(extend-protocol HLRequestBody
  String
  (to-ring-request-body [^String body base64?]
    (io/input-stream
     (if-not base64?
       (.getBytes body)
       (.decode ^Base64$Decoder @base64-decoder ^String body))))

  Object
  (to-ring-request-body [^Object body base64?]
    (pr-str body))

  nil
  (to-ring-request-body [_ _]
    nil))

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
    {:server-port    (some-> (get headers "x-forwarded-port") (Integer/parseInt))
     :body           (to-ring-request-body (:body event) base64?)
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
        {:keys [body encoded?]} (to-hl-response-body body)]
    {:statusCode      (:status response)
     :body            body
     :isBase64Encoded encoded?
     :headers         (:headers response)}))

(defn wrap-hl-req-res-model
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

  (def HttpApiProxyGateway (hlra/wrap-hl-req-res-model ring-handler))

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

  (def HttpApiProxyGateway (hlra/wrap-hl-req-res-model muuntaja-ring-handler))

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
