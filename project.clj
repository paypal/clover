(defproject CLOVER "0.9.0"
  :description "CLOVER - a clever clojure bot framework"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [metosin/compojure-api "1.1.11"]
                 [ring-logger "1.0.1"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [com.grammarly/perseverance "0.1.2"]
                 [clojail "1.0.6"]
                 [clj-http "2.3.0"]
                 [cheshire "5.9.0"]
                 [environ "1.1.0"]
                 [stylefruits/gniazdo "1.1.1" :exclusions [org.eclipse.jetty/jetty-http]]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [throttler "1.0.0"]
                 [http-kit "2.3.0"]
                 [instaparse "1.4.10"]
                 [reduce-fsm "0.1.4"]
                 [org.clojure/core.cache "0.7.2"]
                 [digest "1.4.4"]
                 [saml20-clj "0.1.3"]
                 ]
  :ring {:handler handler/app :init core/clover}
  :uberjar-name "CLOVER.jar"
  :main core
  :test-paths ["test"]
  :jvm-opts ["-Djava.security.manager" "-Djava.security.policy==java.policy"]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]
                   :plugins [[lein-ring "0.12.0"]]}}
            )
