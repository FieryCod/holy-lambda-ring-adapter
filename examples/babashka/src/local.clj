(ns local
  (:require
   [handler :as handler]
   [org.httpkit.server :as srv]))


(def server (srv/run-server handler/router {:port 3000}))
