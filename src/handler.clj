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
            [digest :as d]))


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
        ;;(str/starts-with? uri "/api/<specific-api>") (sso-or-nop-fn)
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
      )))

(def app
  (->> (handler (-> co/config :sso))
       (wrap-auth (-> co/config :sso) (-> co/config :hashed-passwords))
       wrap-session
       logger/wrap-with-logger))
