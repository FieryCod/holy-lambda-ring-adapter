(ns core
  (:require
   [handler :as handler]
   [fierycod.holy-lambda-ring-adapter.core :as hlra]
   [fierycod.holy-lambda.core :as h]))

(def HttpApiGatewayProxy (hlra/ring<->hl-middleware handler/router))

(h/entrypoint [#'HttpApiGatewayProxy])
