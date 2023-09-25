(ns simple-web.logging
  (:require [com.brunobonacci.mulog :as u]))

(defn trace-wrap [handler]
  (fn [req]
    (u/trace
     ::request [:req (select-keys req [:request-method :uri])]
     (handler req))))

(def trace-middleware
  {:name ::tracing-middleware
   :wrap trace-wrap})