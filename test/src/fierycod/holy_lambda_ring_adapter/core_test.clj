(ns fierycod.holy-lambda-ring-adapter.core-test
  (:require
   [muuntaja.core :as m]
   [reitit.coercion.malli :as rcm]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.ring.middleware.muuntaja :as rrmm]
   [reitit.dev.pretty :as pretty]
   [clojure.test :as t]
   [fierycod.holy-lambda-ring-adapter.core :as hra]))

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
  [request respond raise]
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
          handler  (ring/ring-handler
                    (ring/router
                     [["/:hello/:world" {:get {:parameters {:path
                                                            [:map
                                                             [:hello string?]
                                                             [:world string?]]}
                                               :handler    (fn [request]
                                                             {:status 200
                                                              :body   "Hello world"})}}]]

                     {:exception pretty/exception
                      :data      {:muuntaja   m/instance
                                  :coercion   rcm/coercion
                                  :middleware [parameters/parameters-middleware
                                               rrmm/format-middleware
                                               rrc/coerce-exceptions-middleware
                                               rrc/coerce-response-middleware
                                               rrc/coerce-request-middleware]}})
                    (ring/create-default-handler
                     {:not-found (constantly {:status 404 :body "Not found" :headers {"content-type" "text/plain"}})}))]
      (t/is (= {:status 404, :body "Not found", :headers {"content-type" "text/plain"}}
               (handler (request->ring-request-test request1))))
      (t/is (= {:status 404, :body "Not found", :headers {"content-type" "text/plain"}}
               (handler (request->ring-request-test request1))))
      (t/is (= {:statusCode      200,
                :body            "Hello world",
                :isBase64Encoded false,
                :headers         nil}
               (hra/ring-response->hl-response (handler (request->ring-request-test request2)))))
      (t/is (= {:statusCode 200,
                :body "hello world HTTP/1.1",
                :isBase64Encoded false,
                :headers {"something" "something"}}
               ((hra/wrap-hl-req-res-model basic-ring-handler) request2)))
      (t/is (= {:statusCode 200,
                :body "hello world HTTP/1.1",
                :isBase64Encoded false,
                :headers {"something" "something"}}
               ((hra/wrap-hl-req-res-model basic-ring-handler-async) request2 identity identity))))))
