(ns simple-web.auth.basic
  (:require [simple-web.auth :as auth]
            [simple-web.utils :as utils]))

(defn auth-store [{:keys [username password] :as opts}]
  (utils/assert-schema [:map
                        [:username :string]
                        [:password :string]]
                       opts
                       "Failed to create hardcoded password store")
  (reify auth/BasicAuthStore
    (has-access? [_this user pass]
      (and (= username user)
           (= password pass)))))