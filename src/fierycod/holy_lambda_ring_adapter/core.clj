(ns fierycod.holy-lambda-ring-adapter.core
  (:import
   [java.io InputStream File ByteArrayInputStream]
   [java.util Base64]
   [clojure.lang ISeq IPersistentMap])
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]))

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

(defn request->ring-request
  [{:keys [event ctx]}]
  (let [request-ctx (event :requestContext)
        http        (request-ctx :http)
        headers     (event :headers)
        base64?     (event :isBase64Encoded)]
    {:server-port    (some-> (get headers "x-forwarded-port") (Integer/parseInt))
     :body           (when-let [body (event :body)]
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

;; (defn AWSHttpApiGatewayProxy
;;   [request]
;;   (let [ring-handler            (:http/router @system)
;;         result                  (ring-handler (request->ring-request request))
;;         ^ResponseBody body      (:body result)
;;         {:keys [body encoded?]} (adapt body)]

;;     (assoc result
;;            :statusCode (:status result)
;;            :isBase64Encoded encoded?
;;            :body body)))
