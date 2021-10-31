(ns handler
  (:require
   [clojure.string :as str]
   [hiccup2.core :refer [html]]
   [clojure.core.match :refer [match]]
   [clojure.java.io :as io]))

(defn logo
  [_request]
  {:body (slurp (io/file "./logo.png"))
   :status 200
   :headers {"content-type" "image/png"}})

(defn router
  [req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match [(:request-method req) paths]
           [:get ["logo"]] (handler/logo req)
           :else {:body (str (html [:html "Welcome!"
                                    [:img {:src "./logo"}]]))})))
