(ns core
  (:require
   [fierycod.holy-lambda.core :as h]))

(defn ExampleLambda
  [request]
  (clojure.pprint/pprint request))

(h/entrypoint [#'ExampleLambda])
