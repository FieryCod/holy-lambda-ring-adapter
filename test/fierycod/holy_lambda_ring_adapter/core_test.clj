(ns fierycod.holy-lambda-ring-adapter.core-test
  (:require
   [clj-http.client :as client]
   [ring.adapter.jetty :as jetty]
   [muuntaja.core :as m]
   [ring.util.response :as response]
   [reitit.coercion.malli :as rcm]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.ring.middleware.muuntaja :as rrmm]
   [reitit.dev.pretty :as pretty]
   [clojure.test :as t]
   [fierycod.holy-lambda-ring-adapter.core :as hra]
   [clojure.java.io :as io]))

(defn bytes-response->string-response
  [response]
  (assoc response :body (new String ^bytes (:body response))))

(defn ->reitit-ring-handler
  [routes]
  (ring/ring-handler
   (ring/router
    routes
    {:exception pretty/exception
     :data      {:muuntaja   m/instance
                 :coercion   rcm/coercion
                 :middleware [parameters/parameters-middleware
                              rrmm/format-middleware
                              rrc/coerce-exceptions-middleware
                              rrc/coerce-response-middleware
                              rrc/coerce-request-middleware]}})
   (ring/create-default-handler
    {:not-found (constantly {:status 404 :body "Not found" :headers {"content-type" "text/plain"}})})))

(defn ring-spec-props
  [request]
  (select-keys request [:server-port
                        :server-name
                        :remote-addr
                        :uri
                        :query-string
                        :scheme
                        :request-method
                        :protocol
                        :headers]))

(defn basic-ring-handler
  [request]
  {:status  200
   :body    (str "hello world " (:protocol request))
   :headers {"something" "something"}})

(defn basic-ring-handler-async
  [request respond _raise]
  (respond {:status  200
            :body    (str "hello world " (:protocol request))
            :headers {"something" "something"}}))

(defn request->ring-request-test
  [hl-request]
  (ring-spec-props (hra/hl-request->ring-request hl-request)))

(t/deftest http-api-basic-1
  (t/testing "should correctly transform request->ring-request #1"
    (let [hl-request {:event
                      {:routeKey              "GET /bb-amd64",
                       :queryStringParameters {:holy-lambda "example", :hello "world"},
                       :pathParameters        {},
                       :cookies               ["totalOrders=76" "merged-cart=1"],
                       :headers
                       {"sec-fetch-site"            "none",
                        "host"                      "localhost:3000",
                        "user-agent"
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36",
                        "cookie"
                        "totalOrders=76; merged-cart=1",
                        "sec-fetch-user"            "?1",
                        "x-forwarded-port"          "3000",
                        "sec-ch-ua-platform"        "\"Linux\"",
                        "connection"                "keep-alive",
                        "upgrade-insecure-requests" "1",
                        "accept"
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
                        "x-forwarded-proto"         "http",
                        "sec-fetch-mode"            "navigate"},
                       :stageVariables        nil,
                       :rawQueryString        "holy-lambda=example&hello=world",
                       :rawPath               "/bb-amd64",
                       :isBase64Encoded       false,
                       :requestContext
                       {:accountId "123456789012",
                        :apiId     "1234567890",
                        :http
                        {:method    "GET",
                         :path      "/bb-amd64",
                         :protocol  "HTTP/1.1",
                         :sourceIp  "127.0.0.1",
                         :userAgent "Custom User Agent String"},
                        :requestId "692eb753-3353-4a03-9321-26376e8d02e7",
                        :routeKey  "GET /bb-amd64",
                        :stage     nil},
                       :version               "2.0",
                       :body                  ""},
                      :ctx {}}]
      (t/is (= {:protocol       "HTTP/1.1",
                :remote-addr    "127.0.0.1",
                :headers
                {"sec-fetch-site"            "none",
                 "host"                      "localhost:3000",
                 "user-agent"
                 "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36",
                 "cookie"                    "totalOrders=76; merged-cart=1",
                 "sec-fetch-user"            "?1",
                 "x-forwarded-port"          "3000",
                 "sec-ch-ua-platform"        "\"Linux\"",
                 "connection"                "keep-alive",
                 "upgrade-insecure-requests" "1",
                 "accept"
                 "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
                 "x-forwarded-proto"         "http",
                 "sec-fetch-mode"            "navigate"},
                :server-port    3000,
                :uri            "/bb-amd64",
                :server-name    "127.0.0.1",
                :query-string   "holy-lambda=example&hello=world",
                :scheme         :http,
                :request-method :get}
               (request->ring-request-test hl-request)))
      (t/is (= {:status  200,
                :body    "hello world HTTP/1.1",
                :headers {"something" "something"}}
               (basic-ring-handler (request->ring-request-test hl-request))))))

  (t/testing "should correctly transform request->ring-request #2"
    (let [request1 {:event
                    {:routeKey              "GET /bb-amd64",
                     :queryStringParameters {:holy-lambda "example", :hello "world"},
                     :pathParameters        {},
                     :cookies               ["totalOrders=76" "merged-cart=1"],
                     :headers
                     {"sec-fetch-site"            "none",
                      "host"                      "localhost:3000",
                      "user-agent"
                      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36",
                      "cookie"
                      "totalOrders=76; merged-cart=1",
                      "sec-fetch-user"            "?1",
                      "x-forwarded-port"          "3000",
                      "sec-ch-ua-platform"        "\"Linux\"",
                      "connection"                "keep-alive",
                      "upgrade-insecure-requests" "1",
                      "accept"
                      "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
                      "x-forwarded-proto"         "http",
                      "sec-fetch-mode"            "navigate"},
                     :stageVariables        nil,
                     :rawQueryString        "holy-lambda=example&hello=world",
                     :rawPath               "/bb-amd64",
                     :isBase64Encoded       false,
                     :requestContext
                     {:accountId "123456789012",
                      :apiId     "1234567890",
                      :http
                      {:method    "GET",
                       :path      "/bb-amd64",
                       :protocol  "HTTP/1.1",
                       :sourceIp  "127.0.0.1",
                       :userAgent "Custom User Agent String"},
                      :requestId "692eb753-3353-4a03-9321-26376e8d02e7",
                      :routeKey  "GET /bb-amd64",
                      :stage     nil},
                     :version               "2.0",
                     :body                  ""},
                    :ctx {}}
          request2 (assoc-in request1 [:event :requestContext :http :path] "/hello/world")
          handler  (->reitit-ring-handler
                    [["/:hello/:world"
                      {:get {:parameters {:path
                                          [:map
                                           [:hello string?]
                                           [:world string?]]}
                             :handler    (fn [_request]
                                           {:status 200
                                            :body   "Hello world"})}}]])]
      (t/is (= {:status 404, :body "Not found", :headers {"content-type" "text/plain"}}
               (handler (request->ring-request-test request1))))
      (t/is (= {:status 404, :body "Not found", :headers {"content-type" "text/plain"}}
               (handler (request->ring-request-test request1))))
      (t/is (= {:statusCode      200,
                :body            "Hello world",
                :isBase64Encoded false,
                :headers         nil}
               (bytes-response->string-response (hra/ring-response->hl-response (handler (request->ring-request-test request2))))))
      (t/is (= {:statusCode      200,
                :body            "hello world HTTP/1.1",
                :isBase64Encoded false,
                :headers         {"something" "something"}}
               (bytes-response->string-response ((hra/wrap-hl-req-res-model basic-ring-handler) request2))))
      (t/is (= {:statusCode      200,
                :body            "hello world HTTP/1.1",
                :isBase64Encoded false,
                :headers         {"something" "something"}}
               (bytes-response->string-response ((hra/wrap-hl-req-res-model basic-ring-handler-async) request2 identity identity)))))))

(t/deftest http-api-json-coerce-1
  (t/testing "json coercion should work"
    (let [request {:event
                   {:routeKey              "POST /<path:proxy>",
                    :queryStringParameters {},
                    :pathParameters        {:proxy "hello"},
                    :headers
                    {"host"              "localhost:3000",
                     "content-type"      "application/json",
                     "accept"            "*/*",
                     "content-length"    "21",
                     "x-forwarded-proto" "http",
                     "x-forwarded-port"  "3000"},
                    :stageVariables        nil,
                    :rawQueryString        "",
                    :rawPath               "/hello",
                    :isBase64Encoded       false,
                    :requestContext
                    {:accountId "123456789012",
                     :apiId     "1234567890",
                     :http
                     {:method    "POST",
                      :path      "/hello",
                      :protocol  "HTTP/1.1",
                      :sourceIp  "127.0.0.1",
                      :userAgent "Custom User Agent String"},
                     :requestId "85f48e37-542e-4fae-afa0-c9ee1fa48a05",
                     :routeKey  "POST /<path:proxy>",
                     :stage     nil},
                    :version               "2.0",
                    :body                  "{\n\t\"hello\": \"world\"\n}"}
                   :ctx {}}
          handler (->reitit-ring-handler
                   [["/hello" {:post {:handler (fn [_request]
                                                 {:status 200
                                                  :body   {:hello      "world"
                                                           :inner-body (:body-params _request)}})}}]])]

      ;; The case where (HL <= 0.6.1) does not automatically decodes the input
      (t/is (= {:statusCode      200,
                :body            "{\"hello\":\"world\",\"inner-body\":{\"hello\":\"world\"}}",
                :isBase64Encoded false,
                :headers         {"Content-Type" "application/json; charset=utf-8"}}
               (bytes-response->string-response ((hra/wrap-hl-req-res-model handler) request))))

      ;; The case where (HL >= 0.6.2) does automatically decodes the input
      (t/is (= {:statusCode      200,
                :body            "{\"hello\":\"world\",\"inner-body\":{\"hello\":\"world\"}}",
                :isBase64Encoded false,
                :headers         {"Content-Type" "application/json; charset=utf-8"}}
               (bytes-response->string-response ((hra/wrap-hl-req-res-model handler) (-> request
                                                                                         (assoc-in [:event :body] "{\n\t\"hello\": \"world\"\n}")
                                                                                         (assoc-in [:event :parsed-body] {:hello "world"})))))))))


(t/deftest http-api-form-coerce-1
  (t/testing "x-www-form-urlencoded coercion should work"
    (let [request {:event
                   {:routeKey              "POST /<path:proxy>",
                    :queryStringParameters {},
                    :pathParameters        {:proxy "hello"},
                    :cookies               [],
                    :headers
                    {"connection"        "close",
                     "content-type"      "application/x-www-form-urlencoded",
                     "accept-encoding"   "gzip, deflate",
                     "content-length"    "11",
                     "host"              "localhost:3000",
                     "x-forwarded-proto" "http",
                     "x-forwarded-port"  "3000"},
                    :stageVariables        nil,
                    :rawQueryString        "",
                    :rawPath               "/hello",
                    :isBase64Encoded       false,
                    :requestContext
                    {:accountId "123456789012",
                     :apiId     "1234567890",
                     :http
                     {:method    "POST",
                      :path      "/hello",
                      :protocol  "HTTP/1.1",
                      :sourceIp  "127.0.0.1",
                      :userAgent "Custom User Agent String"},
                     :requestId "af9f7839-2aa5-4aef-94fd-111b267f5dfa",
                     :routeKey  "POST /<path:proxy>",
                     :stage     nil},
                    :version               "2.0",
                    :body                  "hello=world"},
                   :ctx
                   {}}
          handler (->reitit-ring-handler
                   [["/hello" {:post {:handler (fn [_request]
                                                 {:status 200
                                                  :body   {:hello       "world"
                                                           :form-params (:form-params _request)}})}}]])]

      (t/is (= {:statusCode      200,
                :body            "{\"hello\":\"world\",\"form-params\":{\"hello\":\"world\"}}",
                :isBase64Encoded false,
                :headers         {"Content-Type" "application/json; charset=utf-8"}}
               (bytes-response->string-response ((hra/wrap-hl-req-res-model handler) request))))
      (t/is (= {:statusCode      200,
                :body
                "{\"hello\":\"world\",\"form-params\":{\"a3\":\"3\",\"a9\":\"9\",\"a7\":\"7\",\"a6\":\"6\",\"a8\":\"8\",\"a\":[\"1\",\"2\",\"3\"],\"a4\":\"4\",\"a1\":\"1\",\"a5\":\"5\",\"a2\":\"2\"}}",
                :isBase64Encoded false,
                :headers         {"Content-Type" "application/json; charset=utf-8"}}
               (bytes-response->string-response ((hra/wrap-hl-req-res-model handler) (assoc-in request [:event :body] "a3=3&a9=9&a7=7&a6=6&a8=8&a4=4&a1=1&a5=5&a2=2&a=1&a=2&a=3")))))
      (t/is (= {:statusCode      200,
                :body
                "{\"hello\":\"world\",\"form-params\":{\"a3\":\"3\",\"a9\":\"9\",\"a7\":\"7\",\"a6\":\"6\",\"a8\":\"8\",\"a\":[\"1\",\"2\",\"3\"],\"a4\":\"4\",\"a1\":\"1\",\"a5\":\"5\",\"Hello World It's Me You Looking For\":\"Hello World ! ! !\",\"a2\":\"2\"}}",
                :isBase64Encoded false,
                :headers         {"Content-Type" "application/json; charset=utf-8"}}
               (bytes-response->string-response ((hra/wrap-hl-req-res-model handler) (assoc-in request [:event :body] "a3=3&a9=9&a7=7&a6=6&a8=8&a4=4&a1=1&a5=5&Hello+World+It%27s+Me+You+Looking+For=Hello+World+%21+%21+%21&a2=2&a=1&a=2&a=3"))))))))

(t/deftest binary-alike-data-response-1
  (t/testing "should correctly base64 encode a file"
    (let [request {:event
                   {:routeKey        "GET /",
                    :rawQueryString  "",
                    :rawPath         "/",
                    :isBase64Encoded false,
                    :requestContext
                    {:http
                     {:method    "GET",
                      :path      "/",
                      :protocol  "HTTP/1.1",
                      :sourceIp  "127.0.0.1",
                      :userAgent "Custom User Agent String"},
                     :requestId "af9f7839-2aa5-4aef-94fd-111b267f5dfa",
                     :routeKey  "GET /",
                     :stage     nil}},
                   :ctx
                   {}}
          file    (io/file "test/fierycod/holy_lambda_ring_adapter/logo.png")
          handler (hra/wrap-hl-req-res-model
                   (->reitit-ring-handler
                    [["/" {:get {:handler (fn [_request]
                                            (response/response file))}}]]))]

      (t/is (= true (:isBase64Encoded (handler request))))
      (t/is (= "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" (new String (:body (handler request)))))))

  (t/testing "should correctly base64 encode a resource"
    (let [request {:event
                   {:routeKey        "GET /",
                    :rawQueryString  "",
                    :rawPath         "/",
                    :isBase64Encoded false,
                    :requestContext
                    {:http
                     {:method    "GET",
                      :path      "/",
                      :protocol  "HTTP/1.1",
                      :sourceIp  "127.0.0.1",
                      :userAgent "Custom User Agent String"},
                     :requestId "af9f7839-2aa5-4aef-94fd-111b267f5dfa",
                     :routeKey  "GET /",
                     :stage     nil}},
                   :ctx
                   {}}
          resource (io/resource "logo.png")
          handler (hra/wrap-hl-req-res-model
                   (->reitit-ring-handler
                    [["/" {:get {:handler (fn [_request]
                                            (response/response resource))}}]]))]
      (t/is (= true (:isBase64Encoded (handler request))))
      (t/is (= "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" (new String (:body (handler request))))))))

(comment
  (def server (jetty/run-jetty (->reitit-ring-handler
                                [["/" {:get {:handler (fn [_request]
                                                        (response/response (io/file "test/src/fierycod/holy_lambda_ring_adapter/logo.png")))}}]])
                               {:join? false
                                :port  3000}))

  (client/post "http://localhost:3000/hello"
               {:accept       :json
                :as           :json
                :body         "{\"hello\": \"world\"}"
                :content-type :json})

  (client/post "http://localhost:3000/hello"
               {:form-params
                (into {:a [1 2 3]
                       "Hello World It's Me You Looking For" "Hello World ! ! !"}
                      (for [i (range 1 10)]
                        [(str "a" i) i]))})

  (.stop server)
  )
