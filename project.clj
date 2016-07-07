(defproject CLOVER "0.8.0"
  :description "CLOVER - "
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [com.grammarly/perseverance "0.1.2"]
                 [compojure "1.4.0"]
                 [clojail "1.0.6"]
                 [clj-http "2.1.0"]
                 [cheshire "5.5.0"]
                 [environ "1.0.2"]
                 [stylefruits/gniazdo "1.0.1"]
                 [ring/ring-defaults "0.1.5"]
                 [throttler "1.0.0"]
                 [http-kit "2.1.19"]
                 [com.taoensso/timbre "4.4.0"]
                 [com.draines/postal "1.11.3"]
                 [instaparse "1.4.2"]
                 [reduce-fsm "0.1.4"]
                 [org.clojure/core.cache "0.6.5"]
                 ]
  :plugins [[lein-ring "0.8.13"]]
  :uberjar-name "CLOVER.jar"
  :main core
  :test-paths ["test"]
  :jvm-opts ["-Djava.security.manager" "-Djava.security.policy==java.policy"];; "-Xss226k";;seems to be the smallest possible
  :profiles {:uberjar {:aot :all}})
