(ns simple-web.base-router
  (:require [muuntaja.core :as m]
            [muuntaja.format.form :as mf]
            [reitit.ring :as r]
            [reitit.coercion.malli :as rm]
            [reitit.ring.coercion :as rc]
            [reitit.ring.middleware.parameters :as rp]
            [reitit.ring.middleware.muuntaja :as mm]
            [reitit.ring.middleware.exception :as re]
            [simple-web.logging :as logging]))

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
            ; Order of middleware is important.
            :middleware [; This ensures query params are handled
                         rp/parameters-middleware
                         ; Format data on the way in and out
                         mm/format-middleware
                         ; This adds exception handling
                         re/exception-middleware
                         ; TODO maybe tracing should not log exceptions here? If so, place at top
                         ; Logging (place below exception handling, or trace will assume the call was a success)
                         logging/trace-middleware
                         ; Data validation middleware
                         rc/coerce-exceptions-middleware
                         rc/coerce-request-middleware
                         rc/coerce-response-middleware]}})
   (r/routes
    (r/redirect-trailing-slash-handler {:method :strip})
    (r/create-resource-handler {:path "/"})
    (r/create-default-handler))))