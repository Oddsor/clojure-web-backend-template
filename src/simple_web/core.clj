(ns simple-web.core
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]
            [juxt.clip.core :as clip])
  (:gen-class))

(defn config [filename]
  (read-config (io/resource filename)))

(defn -main [& _args]
  (clip/start (:system (config "config.edn")))
  @(future))