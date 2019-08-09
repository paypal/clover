(ns handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.string :as str]
            [config :as co]
            [ring.logger :as logger]
            [saml20-clj.routes :as sr]
            [ring.middleware.session :refer :all]
            [ring.middleware.params :refer :all]
            [digest :as d]
            db
            config
            slack-rtm))


;; In ring middleware, "The root cause here is that in Compojure, the routing tree is an application where the middleware are applied in-place - there is no good way to delay the computation to see if the route will eventually match.", hence manual check. `reroute-middleware` might work better.
(defn wrap-auth[sso-config hashed-passwords handler]
  (fn [request]
    (let [uri (request :uri)
          nop-fn #(handler request)
          sso-fn #(if (-> request :session :saml nil?) (sr/redirect-to-saml "/") (nop-fn))
          sso-or-nop-fn #(if (nil? sso-config) (nop-fn) (sso-fn))
          basic-auth-fn #(let [auth-header (get (:headers request) "authorization")]
                           (cond
                             (nil? auth-header) (-> (unauthorized) (header "WWW-Authenticate" "Basic realm=\"whatever\""))
                             (contains? hashed-passwords (d/sha-1 auth-header)) (nop-fn)
                             :else (unauthorized "Access Denied")))
          sso-and-basic-auth-fn #(if (-> request :session :saml nil?) (sr/redirect-to-saml "/") (basic-auth-fn))
          sso-or-sso-and-basic-auth-fn #(if (nil? sso-config) (basic-auth-fn) (sso-and-basic-auth-fn))]
      (cond
        (str/starts-with? uri "/api/lookup") (sso-or-nop-fn)
        (str/starts-with? uri "/saml") (nop-fn)
        (str/starts-with? uri "/api") (sso-or-sso-and-basic-auth-fn)
        :else (sso-or-nop-fn)))))

(defn handler[sso-config]
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "Clover"
                    :description "front end"}
             :tags [{:name "api", :description "try me!"}]
             :securityDefinitions {:BasicAuth
                                   {:type "basic"}}}}}

    (when sso-config (sr/saml-routes sso-config))

    (context "/api" []
             :tags ["api"]
             (GET "/lookup/:term" [term]
                  :tags [:db]
                  :return s/Any
                  :summary "looks up clover DB, WIP"
                  (ok (db/lookup term)))
             (GET "/nuke" {session :session}
                  :tags [:system]
                  :return {:comment String s/Keyword s/Any}
                  :summary "nuke clover, hopefully the driving scripts will restart it"
                  (do
                    (when sso-config
                      (let [a (->> session :saml :assertions first :attrs)
                            msg (format "Hey! %s from %s (as %s) nuked clover, it should restart soon. Don't panic, stay calm!"
                                        (-> "displayName" a first)
                                        (-> "department" a first)
                                        (-> "title" a first))]
                        (slack-rtm/run-api-get (:api-token config/config) "chat.postMessage" {:channel (:dev-channel config/config) :text msg})))
                    (future (Thread/sleep 1000)
                            (System/exit 0))
                    (ok {:comment "the system will shut down momentarily"}))
                  )
             (GET "/test" {session :session}
                  :tags [:system]
                  :return {:comment String s/Keyword s/Any}
                  :summary "test system api"
                  (do
                    (when sso-config
                      (let [a (->> session :saml :assertions first :attrs)
                            msg (format "Hey! %s from %s (as %s) : you can access system API now"
                                        (-> "displayName" a first)
                                        (-> "department" a first)
                                        (-> "title" a first))]
                        (slack-rtm/run-api-get (:api-token config/config) "chat.postMessage" {:channel (:dev-channel config/config) :text msg})))
                    (ok {:comment "you can access system API now"}))
                  )

             )))

(def app
  (->> (handler (-> co/config :sso))
       (wrap-auth (-> co/config :sso) (-> co/config :hashed-passwords))
       wrap-session
       logger/wrap-with-logger))
