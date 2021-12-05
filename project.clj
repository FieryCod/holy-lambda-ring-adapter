(defproject io.github.FieryCod/holy-lambda-ring-adapter "0.1.1"
  :description "An adapter between Ring Core request/response model and Holy Lambda. Run Ring applications on AWS Lambda"

  :url "https://github.com/FieryCod/holy-lambda-ring-adapter"

  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}

  :source-paths ["src" "test"]

  :resources ["resources"]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [ring/ring-core "1.9.4" :scope "provided"]]

  :deploy-repositories [["releases" {:url   "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url   "https://clojars.org/repo"
                                      :creds :gpg}]]
  :scm {:name "git"
        :url  "https://github.com/FieryCod/holy-lambda-ring-adapter"}


  :profiles {:dev {:dependencies [[eftest/eftest "0.5.9"]
                                  [io.github.FieryCod/holy-lambda "0.5.1-SNAPSHOT"]
                                  [cheshire/cheshire "5.10.0"]
                                  [metosin/muuntaja "0.6.8"]
                                  [metosin/reitit "0.5.15"]
                                  [clj-http/clj-http "3.12.3"]]}})
