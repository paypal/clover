(defproject CLOVER "0.8.1"
  :description "CLOVER - a clever clojure bot framework"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [com.grammarly/perseverance "0.1.2"]
                 [clojail "1.0.6"]
                 [clj-http "2.3.0"]
                 [cheshire "5.9.0"]
                 [environ "1.1.0"]
                 [stylefruits/gniazdo "1.1.1"]
                 [throttler "1.0.0"]
                 [http-kit "2.3.0"]
                 [instaparse "1.4.10"]
                 [reduce-fsm "0.1.4"]
                 [org.clojure/core.cache "0.7.2"]
                 ]
  :plugins [[lein-ring "0.8.13"]]
  :uberjar-name "CLOVER.jar"
  :main core
  :test-paths ["test"]
  :jvm-opts ["-Djava.security.manager" "-Djava.security.policy==java.policy"]
  :profiles {:uberjar {:aot :all}})
