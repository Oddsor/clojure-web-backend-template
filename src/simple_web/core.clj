(ns simple-web.core
  (:require [aero.core :refer [read-config reader]]
            [clojure.java.io :as io]
            [integrant.core :as ig])
  (:gen-class))

; aero-configs do not understand the integrant/ref tag,
; so we extend with a simple handler
(defmethod reader 'ig/ref [_opts _tag value]
  (ig/ref value))

(defn config [filename]
  (read-config (io/resource filename)))

(defn init-system
  {:malli/schema [:=> [:cat :map] :map]}
  [sys-config]
  (ig/load-namespaces sys-config)
  (ig/init sys-config))

(defn -main [& _args]
  (init-system (:system (config "config.edn")))
  @(future))