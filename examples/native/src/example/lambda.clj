(ns example.lambda
  (:gen-class)
  (:require
   [example.routes :as routes]
   [fierycod.holy-lambda.core :as h]
   [com.stuartsierra.component :as component]
   [fierycod.holy-lambda-ring-adapter.core :as hra]))

(def handler (:ring-handler (component/start (routes/->ring-handler-component {}))))
(def HttpAPIProxyGateway (hra/ring<->hl-middleware handler))

(h/entrypoint [#'HttpAPIProxyGateway])
