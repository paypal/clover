(ns evaluator
  (:require [clojail.core :refer [sandbox]]
            [clojail.testers :refer [secure-tester-without-def blanket]])
  (:import java.io.StringWriter))

(def sb (sandbox (conj secure-tester-without-def (blanket "CLOVER")) :max-defs 1000 :timeout 250))

;;anything with "_tree:" is a hack here

(defn remap-emojis[e]
  (-> e
      (.replaceAll ":north_tree:" "north_tree")
      (.replaceAll ":west_tree:" "west_tree")
      (.replaceAll ":south_tree:" "south_tree")
      (.replaceAll ":east_tree:" "east_tree")
      (.replaceAll "&gt;" ">")
      (.replaceAll "&lt;" "<")))

(defn format-result [r]
  (if (:status r)
    (if-not (.startsWith (:output r) "```")
      (str "```"
           (when-not (.contains (:input r) "_tree:")
             (str "=> " (:form r) "\n"))
         (when-let [o (:output r)]
           o)
         (if-not (nil? (:result r))
           (:result-str r))
         "```")
      (.substring (:output r) 3)
      )
    (str "```"
         "==> " (or (:form r) (:input r)) "\n"
         (or (:result r) "Unknown Error")
         "```")))

(defn eval-expr
  "Evaluate the given string"
  [s]
  (try
    (with-open [out (StringWriter.)]
      (let [form (binding [*read-eval* false] (read-string (remap-emojis s)))
            result (sb form {#'*out* out})]
        {:status true
         :input s
         :form form
         :result result
         :result-str (with-out-str (pr result))
         :output (.toString out)}))
    (catch Exception e
      {:status false
       :input s
       :result (.getMessage e)})))

(defn evaluate [s] (-> s eval-expr format-result))

(defn split-source[s] (clojure.string/split s #";;CUT"))

(->> "src/tree.clj" slurp split-source rest (map evaluate) doall)
