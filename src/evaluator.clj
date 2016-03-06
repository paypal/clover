(ns evaluator
  (:require [clojail.core :refer [sandbox]]
            [clojail.testers :refer [secure-tester-without-def blanket]]
            [util])
  (:import java.io.StringWriter))

(def sb (sandbox (conj secure-tester-without-def (blanket "CLOVER"))))

(defn eval-expr
  "Evaluate the given string"
  [s]
  (try
    (with-open [out (StringWriter.)]
      (let [form (binding [*read-eval* false] (read-string s))
            result (sb form {#'*out* out})]
        {:status true
         :input s
         :form form
         :result (with-out-str (pr result))
         :output (.toString out)}))
    (catch Exception e
      {:status false
       :input s
       :result (.getMessage e)})))

(defn format-result [r]
  (if (:status r)
    (str "```"
         "=> " (:form r) "\n"
         (when-let [o (:output r)]
           o)
         (if (nil? (:result r))
           "nil"
           (:result r))
         "```")
    (str "```"
         "==> " (or (:form r) (:input r)) "\n"
         (or (:result r) "Unknown Error")
         "```")))

;;## move to separate module
(defn evaluate [s] (-> s last eval-expr util/format-result-for-slack))
