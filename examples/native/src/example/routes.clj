(ns example.routes
  (:require
   [reitit.coercion.malli :as coercion-malli]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [muuntaja.core :as m]
   [ring.util.response :as response]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [ring.util.io :as ring-io]
   [com.stuartsierra.component :as component]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring :as ring]))

(defn- ring-handler
  [_dependencies]
  (ring/ring-handler
   (ring/router
    [["/resources/*" {:no-doc true
                      :get    {:handler (ring/create-resource-handler)}}]
     ["/welcome-screen" {:no-doc true
                         :get    {:handler (fn [_req]
                                             {:body    "<html><iframe width=\"100%\" height=\"100%\" src=\"/resources/index.html\"></iframe></html>"
                                              :status  200
                                              :headers {"content-type" "text/html"}})}}]
     ["/api/v1" {:swagger {:info {:title "Application routes"
                                  :description "Lorem ipsum"}}}
      ["/seq" {:get {:handler (fn [_req]
                                {:body   '("hello" "world")
                                 :status 200})}}]
      ["/byte-array-hello" {:get {:handler (fn [_req]
                                             {:body   (ring-io/string-input-stream "Hello world" "utf-8")
                                              :status 200})}}]
      ["/hello" {:description "Says Hello!"
                 :get         {:handler (fn [_req]
                                          (response/response {:hello "Hello world"}))}}]
      ["/say-hello" {:description "Now it's your turn to say hello"
                     :post        {:parameters {:body [:map
                                                       [:hello string?]]}
                                   :handler    (fn [{:keys [parameters]}]
                                                 (response/response {:hello (:body parameters)}))}}]]
     ["" {:no-doc true}
      ["/swagger.json" {:get {:swagger {:info     {:title       "Ring API"
                                                   :description "Ring API running on AWS Lambda"
                                                   :version     "1.0.0"}}
                              :handler (swagger/create-swagger-handler)}}]
      ["/api-docs/*" {:get (swagger-ui/create-swagger-ui-handler)}]]]
    {:data {:coercion   coercion-malli/coercion
            :muuntaja   m/instance
            :middleware [;; query-params & form-params
                         parameters/parameters-middleware
                         ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                         ;; encoding response body
                         muuntaja/format-response-middleware


                         ;; exception handling
                         exception/exception-middleware
                         ;; decoding request body
                         muuntaja/format-request-middleware
                         ;; coercing response bodys
                         coercion/coerce-response-middleware
                         ;; coercing request parameters
                         coercion/coerce-request-middleware
                         ;; multipart
                         multipart/multipart-middleware
                         ]}})
   (ring/create-default-handler
    {:not-found (constantly {:status 404
                             :body   "Not found route!"})})))

(defrecord ^:private RingHandlerComponent [dependencies]
  component/Lifecycle
  (start [this]
    (assoc this :ring-handler (ring-handler dependencies)))
  (stop [this]
    (dissoc this :ring-handler)))

(defn ->ring-handler-component
  [opts]
  (map->RingHandlerComponent opts))
