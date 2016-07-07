(ns commands
  (:require [clojure.string :as string]
            [instaparse.core :as insta]
            [diagnostics :as dia]
            [expander]))

(dia/init)

(def read-clover-lang (insta/parser
    "clover-sentence = eval / define / explain / help / noop
     help = '!help'
     define = ('?' | '!define') break term break '=' break #'.+'
     explain = ('?' | '!explain') break term break
     term = word space term | word
     space = #'\\s+'
     word = #'[a-zA-Z0-9&-/]+'
     eval = #'[\\'\u2018]' catchall
     noop = catchall
     break = #'\\s*'
     catchall = #'([\\s\\S])*'"))


(def eval-stage (partial insta/transform
                         {
                             :clover-sentence identity
                             :help expander/help
                             :word identity
                             :space (fn [_] " ")
                             :term str
                             :define (fn [_ _ term _ _ _ definition] (expander/teach term definition))
                             :explain (fn [_ _ term _] (expander/lookup term))
                             :catchall identity
                             :eval (fn [_ cmd] (vector false (expander/evaluate cmd)))
                             :noop (fn [_] nil)
                         }))

(defn parse-and-execute [s]
    (if (= "?WF*$" s)
        [false "Working from Starbucks."]
        (let [tree (read-clover-lang s)]
            (if (insta/failure? tree)

(defn parse-and-execute[s]
  (try
    (cond
      (= "?WF*$" s) [false "Working from Starbucks."]
      :else (let [tree (read-clover-lang s)]
              (if (insta/failure? tree)
                (prn ">>> Insta parse failure: " tree)
                (eval-stage tree))))
    (catch java.lang.Throwable  e2 (do (println "ERROR for " (pr-str s) "\n"  e2)(error e2)))
    )
  )
