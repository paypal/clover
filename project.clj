(defproject CLOVER "0.7.1"
  :description "CLOVER - "
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [compojure "1.4.0"]
                 [clojail "1.0.6"]
                 [clj-http "2.1.0"]
                 [cheshire "5.5.0"]
                 [environ "1.0.2"]
                 [stylefruits/gniazdo "0.4.1"]
                 [ring/ring-defaults "0.1.5"]
                 [throttler "1.0.0"]
                 [http-kit "2.1.19"]
                 [instaparse "1.4.2"]
                 ]
  :plugins [[lein-ring "0.8.13"]]
  :uberjar-name "CLOVER.jar"
  :main core
  :test-paths ["test"]
  :jvm-opts ["-Djava.security.manager" "-Djava.security.policy==java.policy"]
  :profiles {:uberjar {:aot :all}})
