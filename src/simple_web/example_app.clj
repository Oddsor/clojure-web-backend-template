(ns simple-web.example-app
  (:require [integrant.core :as ig]
            [simple-web.base-router :as br]
            [com.brunobonacci.mulog :as u]
            [selmer.parser :as selmer]
            [rum.core :as rum]))

(def root-input-spec [:map
                      [:name {:optional true} :any]])

(def router
  [["/" {:get {:parameters {:query root-input-spec}
               :handler
               (fn [req]
                 (u/trace
                  ::root-get [:req req]
                  (let [hello-name (or (-> req :parameters :query :name) "world")]
                    {:status 200
                     :body (format "Hello, %s!" hello-name)})))}
         :post {:parameters {:form root-input-spec
                             :body root-input-spec}
                :handler (fn [req]
                           (u/trace
                            ::root-post [:req req]
                            (let [hello-name (or (-> req :parameters :form :name)
                                                 (-> req :parameters :body :name)
                                                 "world")]
                              {:status 200
                               :body (format "Hello, %s!" hello-name)})))}}]
   ["/:name"
    {:get {:parameters {:path root-input-spec}
           :handler (fn [req]
                      (u/trace
                       ::root-get-path [:req req]
                       (let [hello-name (or (-> req :parameters :path :name) "world")]
                         {:status 200
                          :body (format "Hello, %s!" hello-name)})))}}]
   ["/selmer/:name"
    {:get {:parameters {:path root-input-spec}
           :handler (fn [req]
                      (u/trace
                       ::root-get-selmer [:req req]
                       (let [hello-name (or (-> req :parameters :path :name) "world")]
                         {:status 200
                          :body (selmer/render-file "templates/selmer-hello.html"
                                                    {:name hello-name})})))}}]
   ["/rum/:name"
    {:get {:parameters {:path root-input-spec}
           :handler (fn [req]
                      (u/trace
                       ::root-get-rum [:req req]
                       (let [hello-name (or (-> req :parameters :path :name) "world")]
                         {:status 200
                          :body (rum/render-static-markup
                                 [:h1 "Hello " hello-name "!!"])})))}}]])

(defn dev-handler
  "This sneaky layer of indirection will ensure that while developing, the
   router will correctly reload when loading the namespace.
   
   For production, this should not be done as it causes the app to perform
   unnecessary work."
  [req]
  ((br/handler router) req))

(defmethod ig/init-key ::handler [_ opts]
  (if (-> opts :dev)
    dev-handler
    (br/handler router)))