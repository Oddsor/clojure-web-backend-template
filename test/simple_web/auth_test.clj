(ns simple-web.auth-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [hato.client :as hato]
            [juxt.clip.core :as clip])
  (:import [no.nav.security.mock.oauth2 MockOAuth2Server OAuth2Config]
           [no.nav.security.mock.oauth2.http Route]
           [no.nav.security.mock.oauth2.token DefaultOAuth2TokenCallback]))

(defonce server-ref (atom nil))
(defn with-oauth-mock-server [f]
  (let [server (MockOAuth2Server. (OAuth2Config.) (into-array Route []))]
    (try
      (reset! server-ref server)
      (.start server)
      (f)
      (finally
        (.shutdown server)
        (reset! server-ref nil)))))
(defn get-well-known-url []
  (str (.. @server-ref (wellKnownUrl "default"))))

(def conf {:components {:jwt {:start nil}
                        :handler {:start (list 'simple-web.example-app/handler {:auth-backend (clip/ref :jwt)})}
                        :server {:start (list 'ring.adapter.jetty/run-jetty
                                              (clip/ref :handler)
                                              {:port 3210
                                               :join? false})
                                 :stop '(.stop this)}}})

(defn with-server
  "Start web server and oauth mock server.
   Oauth-server needs to start first so that we can use its well-known url to inject into
   our web server"
  [f]
  (with-oauth-mock-server
   (fn []
     (let [conf (assoc-in conf
                          [:components :jwt :start] (list 'simple-web.auth/jwt-backend (get-well-known-url)))
           _ (clip/require conf)
           system (clip/start conf)]
       (try
         (f)
         (finally
           (clip/stop conf system)))))))

(defn get-access-token
  "Abstract away Java-specifics for getting an access token from the mock server"
  []
  (.. @server-ref
      (issueToken "default" "clientid" (DefaultOAuth2TokenCallback.))
      serialize))

(use-fixtures :once with-server)

(deftest get-well-known-info
  (is (= 200 (-> (hato/get (get-well-known-url)) :status))))

(deftest is-authorized-test
  (is (= 401 (-> (hato/get "http://localhost:3210/auth" {:throw-exceptions? false}) :status))
      "Not authorized when no token is supplied")

  (is (= 200 (-> (hato/get "http://localhost:3210/auth"
                           {:headers {"Authorization" (str "Bearer " (get-access-token))}
                            :throw-exceptions? false}) :status))
      "Authorized when token is supplied")
  
  (is (= 200 (-> (hato/get "http://localhost:3210"
                           {:headers {"Authorization" (str "Bearer blabla")}
                            :throw-exceptions? false}) :status))
      "Does not complain when unprotected route carries a bad auth header"))