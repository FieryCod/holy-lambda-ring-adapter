(defproject io.github.FieryCod/holy-lambda-ring-adapter "0.0.1"
  :description "Adapter between Ring<->Holy Lambda"

  :url "https://github.com/FieryCod/holy-lambda-ring-adapter"

  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}

  :source-paths ["src"]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]]

  :eftest {:thread-count 4}

  :plugins [[lein-cloverage "1.1.1"]]

  :deploy-repositories [["releases" {:url   "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url   "https://clojars.org/repo"
                                      :creds :gpg}]]
  :scm {:name "git"
        :url  "https://github.com/FieryCod/holy-lambda-ring-adapter"}

  :cloverage {:runner      :eftest
              :runner-opts {:test-warn-time 500
                            :fail-fast?     true
                            :multithread?   :namespaces}}

  :profiles {:eftest  {:resource-paths ["resources-test"]
                       :global-vars    {*warn-on-reflection* true}
                       :dependencies   [[eftest/eftest "0.5.9"]
                                        [ring "1.9.4"]]
                       :plugins        [[lein-eftest "0.5.9"]]}
             :uberjar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
