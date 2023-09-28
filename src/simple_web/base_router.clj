(ns simple-web.base-router
  (:require [muuntaja.core :as m]
            [muuntaja.format.form :as mf]
            [reitit.coercion.malli :as rm]
            [reitit.ring :as r]
            [reitit.ring.coercion :as rc]
            [reitit.ring.middleware.dev :as dev]
            [reitit.ring.middleware.exception :as re]
            [reitit.ring.middleware.muuntaja :as mm]
            [reitit.ring.middleware.parameters :as rp]
            [simple-web.auth :as auth]
            [simple-web.logging :as logging]))

(def expose-opts-middleware
  {:name ::opts
   :compile (fn [_ opts]
              (when-let [opts (-> opts :data :opts)]
                (fn [handler]
                  (fn [req]
                    (handler (assoc req :opts opts))))))})

(defn base-handler
  {:malli/schema [:function
                  [:=> [:cat :any] :any]
                  [:=> [:cat :any :any] :any]]}
  ([router] (base-handler router {}))
  ([router opts]
   (r/ring-handler
    (r/router
     router
     (cond-> {:data {:muuntaja (m/create
                       ; Handling form parameters
                                (let [form-format mf/format]
                                  (assoc-in m/default-options
                                            [:formats (:name form-format)]
                                            form-format)))
                     :opts opts
                     :coercion rm/coercion
            ; Order of middleware is important.
                     :middleware [; This adds exception handling.
                         ; Default handler returns a clojure-map which won't be correctly formatted,
                         ; so we override that one. (muuntaja would handle maps, but can't be placed above the
                         ; exception middleware because otherwise formatting errors won't be caught!)
                                  (re/create-exception-middleware
                                   (assoc re/default-handlers
                                          ::re/default (fn [^Exception e _]
                                                         {:status 500
                                                          :body (str "Unknown error occurred: " (ex-message e))})))
                         ; Authentication and authorization (inactive if no backend is supplied in options)
                                  auth/auth-middleware
                                  expose-opts-middleware
                         ; This ensures query params are handled 
                                  rp/parameters-middleware
                         ; Format data on the way in and out
                                  mm/format-middleware
                         ; TODO maybe tracing should not log exceptions here? If so, place at top
                         ; Logging (place BELOW exception handling, or trace will assume the call was a success)
                                  logging/trace-middleware
                         ; Data validation middleware
                                  rc/coerce-exceptions-middleware
                                  rc/coerce-request-middleware
                                  rc/coerce-response-middleware]}}
       (:dev opts) (assoc :reitit.middleware/transform dev/print-request-diffs)))
    (r/routes
     (r/redirect-trailing-slash-handler {:method :strip})
     (r/create-resource-handler {:path "/"})
     (r/create-default-handler)))))