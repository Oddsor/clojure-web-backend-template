(ns simple-web.auth
  (:require [buddy.auth :as ba]
            [buddy.auth.backends.httpbasic :as basic]
            [buddy.auth.backends.token :as bt]
            [buddy.auth.middleware :as mw]
            [buddy.core.keys :as bk]
            [buddy.sign.jws :as jws]
            [buddy.sign.jwt :as jwt]
            [hato.client :as hato]
            [integrant.core :as ig]
            [jsonista.core :as json] 
            [ring.util.response :as response]
            [simple-web.utils :as utils]))

(defn- json [json-str]
  (json/read-value json-str (json/object-mapper {:decode-key-fn true})))

(defn- get-jwk-keys [well-known-url]
  (let [well-known-data (-> (hato/get well-known-url) :body json)
        jwks (-> (hato/get (:jwks_uri well-known-data)) :body json :keys)]
    (into {} (map (juxt :kid bk/jwk->public-key)) jwks)))

(defn logged-in-user [req]
  (if-let [id (-> req :identity)]
    id
    (ba/throw-unauthorized)))

(defn- handle-unauthorized [_req errors]
  (-> (response/response "Unauthorized")
      ;; TODO: Human error messages?
      (response/header "WWW-Authenticate" errors)
      (response/status 401)))

(defn- handle-basic-unauthorized [req errors]
  (-> (handle-unauthorized req errors)
      (response/header "WWW-Authenticate" (str "Basic realm=" (:realm errors)))))

(defn- handle-auth [jwk-pubkeys _req token]
  (try
    (let [{:keys [kid]} (jws/decode-header token)]
      (jwt/unsign token (get jwk-pubkeys kid) {:alg :rs256}))
    (catch Exception e (ba/throw-unauthorized (ex-data e)))))

(defn jwt-backend [well-known-url]
  (bt/token-backend {:authfn (partial handle-auth (get-jwk-keys well-known-url))
                     :unauthorized-handler handle-unauthorized
                     :token-name "Bearer"}))

(defprotocol BasicAuthStore
  (set-access! [this username password] "Set password for a user")
  (revoke! [this username] "Revoke access")
  (has-access? [this username password] "Check if user has access"))

(defmethod ig/init-key ::jwt-backend [_ {:keys [well-known-url]}]
  (jwt-backend well-known-url))

(defmethod ig/init-key ::basic-backend [_ {:keys [realm password-store] :as opts}]
  (utils/assert-schema [:map
                        [:realm {:optional true} :string]
                        [:password-store [:fn {:error/message "must implement the BasicAuthStore protocol"} (partial satisfies? BasicAuthStore)]]]
                       opts
                       "Failed to create basic auth backend")
  (basic/http-basic-backend
   {:realm (or realm "simple-web")
    :authfn (fn [request authdata]
              (let [username (:username authdata)
                    password (:password authdata)]
                (when (has-access? password-store username password)
                  {:name username})))
    :unauthorized-handler handle-basic-unauthorized}))

(def auth-middleware
  {:name ::auth
   :compile (fn [{:keys [auth]} opts]
              (when-let [auth-backend (and auth (-> opts :data :opts :auth-backend))]
                (fn [handler]
                  (-> handler
                      (mw/wrap-authentication auth-backend)
                      (mw/wrap-authorization auth-backend)))))})
