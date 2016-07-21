(ns diagnostics
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders (postal :as postal-appender)])
  (:use [config]))

(def log-file-name "log.txt")
;;(io/delete-file log-file-name :quiet)

(timbre/merge-config! {:appenders {:postal (postal-appender/postal-appender {:host "<config>" :from "<config>" :to (:email-log-to config)})}})
(timbre/merge-config! {:appenders {:postal {:rate-limit nil :async? false}}})
(timbre/merge-config! {:appenders {:spit (appenders/spit-appender {:fname log-file-name})}})
;;(timbre/merge-config! {:appenders {:spit {:min-level :error}}})

(timbre/set-level! :warn)

(defn init[] (timbre/refer-timbre))
