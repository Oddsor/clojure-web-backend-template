(ns simple-web.logging
  (:require [com.brunobonacci.mulog :as u]
            [integrant.core :as ig]))

(defn trace-wrap [handler]
  (fn [req]
    (u/trace
     ::request [:req (select-keys req [:request-method :uri])]
     (handler req))))

(def trace-middleware
  {:name ::tracing-middleware
   :wrap trace-wrap})

(defmethod ig/init-key ::console [_ _opts]
  (u/start-publisher! {:type :console}))

(defmethod ig/halt-key! ::console [_ publisher]
  (publisher))