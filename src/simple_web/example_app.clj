(ns simple-web.example-app
  (:require [rum.core :as rum]
            [selmer.parser :as selmer]
            [simple-web.auth :as auth]
            [simple-web.base-router :as br]))

(def root-input-spec [:map
                      [:name {:optional true} :any]])

(def router
  [["/" {:get {:parameters {:query root-input-spec}
               :handler
               (fn [req]
                 (let [hello-name (or (-> req :parameters :query :name) "world")]
                   {:status 200
                    :body (format "Hello, %s!" hello-name)}))}
         :post {:parameters {:form root-input-spec
                             :body root-input-spec}
                :handler (fn [req]
                           (let [hello-name (or (-> req :parameters :form :name)
                                                (-> req :parameters :body :name)
                                                "world")]
                             {:status 200
                              :body (format "Hello, %s!" hello-name)}))}}]
   ["/path/:name"
    {:get {:parameters {:path root-input-spec}
           :handler (fn [req]
                      (let [hello-name (or (-> req :parameters :path :name) "world")]
                        {:status 200
                         :body (format "Hello, %s!" hello-name)}))}}]
   ["/auth" {:get {:parameters {:query root-input-spec}
                   ; TODO need to declare this to enable authentication, but also need to throw unauthorized-exceptions in handler to actually block users. How do we prevent unnecessary token validation if we remove the auth-keyword...?
                   :auth true
                   :handler
                   (fn [req]
                     (println (auth/logged-in-user req))
                     {:status 200
                      :body (format "Hello, %s!" (-> (auth/logged-in-user req) :name))})}}]
   ["/selmer/:name"
    {:get {:parameters {:path root-input-spec}
           :handler (fn [req]
                      (let [hello-name (or (-> req :parameters :path :name) "world")]
                        {:status 200
                         :body (selmer/render-file "templates/selmer-hello.html"
                                                   {:name hello-name})}))}}]
   ["/rum/:name"
    {:get {:parameters {:path root-input-spec}
           :handler (fn [req]
                      (let [hello-name (or (-> req :parameters :path :name) "world")]
                        {:status 200
                         :body (rum/render-static-markup
                                [:h1 "Hello " hello-name "!!"])}))}}]])

(defn handler [{:keys [dev] :as opts}]
  (if dev
    ;;"This sneaky layer of indirection will ensure that while developing, the
    ;; router will correctly reload when loading the namespace.
    ;; For production, this should not be done as it causes the app to perform
    ;; unnecessary work."
    (fn [req] ((br/base-handler router opts) req))
    (br/base-handler router opts)))