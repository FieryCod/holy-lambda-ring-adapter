(ns fierycod.holy-lambda-ring-adapter.codec
  "Functions for encoding and decoding data. All credits to @weavejester.
  https://github.com/ring-clojure/ring-codec"
  (:require [clojure.string :as str])
  (:import
   java.util.Map
   [java.net URLEncoder]))

(defn- double-escape [^String x]
  (.replace (.replace x "\\" "\\\\") "$" "\\$"))

(def ^:private string-replace-bug?
  (= "x" (str/replace "x" #"." (fn [_x] "$0"))))

(defmacro ^:no-doc fix-string-replace-bug [x]
  (if string-replace-bug?
    `(double-escape ~x)
    x))

(defn- parse-bytes ^bytes [encoded-bytes]
  (let [encoded-len (count encoded-bytes)
        bs (byte-array (/ encoded-len 3))]
    (loop [encoded-index 1, byte-index 0]
      (if (< encoded-index encoded-len)
        (let [encoded-byte (subs encoded-bytes encoded-index (+ encoded-index 2))
              b (.byteValue (Integer/valueOf encoded-byte 16))]
          (aset bs byte-index b)
          (recur (+ encoded-index 3) (inc byte-index)))
        bs))))

(defprotocol ^:no-doc FormEncodeable
  (form-encode* [x encoding]))

(extend-protocol FormEncodeable
  String
  (form-encode* [^String unencoded ^String encoding]
    (URLEncoder/encode unencoded encoding))
  Map
  (form-encode* [params encoding]
    (letfn [(encode [x] (form-encode* x encoding))
            (encode-param [k v] (str (encode (name k)) "=" (encode v)))]
      (->> params
           (mapcat
            (fn [[k v]]
              (cond
                (sequential? v) (map #(encode-param k %) v)
                (set? v)        (sort (map #(encode-param k %) v))
                :else           (list (encode-param k v)))))
           (str/join "&"))))
  Object
  (form-encode* [x encoding]
    (form-encode* (str x) encoding))
  nil
  (form-encode* [_ __] ""))

(defn form-encode
  "Encode the supplied value into www-form-urlencoded format, often used in
  URL query strings and POST request bodies, using the specified encoding.
  If the encoding is not specified, it defaults to UTF-8"
  ([x]
   (form-encode x "UTF-8"))
  ([x encoding]
   (form-encode* x encoding)))
