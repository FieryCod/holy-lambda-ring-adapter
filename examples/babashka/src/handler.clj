(ns handler
  (:require
   [clojure.string :as str]
   [hiccup2.core :refer [html]]
   [clojure.core.match :refer [match]]
   [clojure.java.io :as io]))

(defn logo
  [_request]
  {:body (io/file "./logo.png")
   :status 200
   :headers {"content-type" "image/png"}})

(defn hello
  [_request]
  {:body {"hello" "world"}
   :status 200
   :headers {"content-type" "application/json"}})

(defn router
  [req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match [(:request-method req) paths]
           [:get ["logo"]] (handler/logo req)
           [:get ["hello"]] (handler/hello req)
           [:get ["welcome"]] {:body (str (html [:html "Welcome!"
                                                 [:img {:src "./logo"}]]))
                               :headers {"content-type" "text/html; charset=utf-8"}
                               :status 200}
           :else {:body "Not Found"
                  :status 404}
           )))
