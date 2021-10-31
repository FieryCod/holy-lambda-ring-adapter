(ns core
  (:require
   [handler :as handler]
   [babashka.process :as p]
   [fierycod.holy-lambda-ring-adapter.core :as hlra]
   [fierycod.holy-lambda.core :as h]))

(defn- shell-no-exit
  [cmd & args]
  (p/process (into (p/tokenize cmd) (remove nil? args)) {:inherit true}))


(def HttpApiGatewayProxy (hlra/wrap-hl-req-res-model handler/router))

(h/entrypoint [#'HttpApiGatewayProxy])
