(ns example.server
  (:require
   [example.routes :as routes]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :as jetty]))

(defonce system nil)

(defrecord Server [ring-handler]
  component/Lifecycle
  (start [this]
    (assoc this :server (jetty/run-jetty (:ring-handler ring-handler)
                                         {:port  3000
                                          :join? false})))
  (stop [this]
    (when-let [server (:server this)]
      (.stop server))
    (dissoc this :server)))

(defn ->server
  [opts]
  (map->Server opts))

(defn ->system
  []
  (-> (component/system-map
       :server (->server {})
       :ring-handler (routes/->ring-handler-component {}))
      (component/system-using
       {:server {:ring-handler :ring-handler}})))

(alter-var-root #'system (fn [x]
                           (when x (component/stop x))
                           (->system)))

(alter-var-root #'system (fn [x] (when x (component/start x))))
