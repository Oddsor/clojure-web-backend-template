(ns simple-web.base-router
  (:require [muuntaja.core :as m]
            [muuntaja.format.form :as mf]
            [reitit.ring :as r]
            [reitit.coercion.malli :as rm]
            [reitit.ring.coercion :as rc]
            [reitit.ring.middleware.parameters :as rp]
            [reitit.ring.middleware.muuntaja :as mm]))

(defn handler [router]
  (r/ring-handler
   (r/router
    router
    {:data {:muuntaja (m/create
                       ; Handling form parameters
                       (let [form-format mf/format]
                         (assoc-in m/default-options
                                   [:formats (:name form-format)]
                                   form-format)))
            :coercion rm/coercion
            :middleware [rp/parameters-middleware
                         mm/format-middleware
                         rc/coerce-exceptions-middleware
                         rc/coerce-request-middleware
                         rc/coerce-response-middleware]}})
   (r/routes
    (r/redirect-trailing-slash-handler {:method :strip})
    (r/create-resource-handler {:path "/"})
    (r/create-default-handler))))