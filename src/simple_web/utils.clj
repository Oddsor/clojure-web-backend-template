(ns simple-web.utils 
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]
            [malli.core :as m]
            [malli.error :as me]))

(defn config [filename]
  (read-config (io/resource filename)))

(defn assert-schema [schema value prefixed-message]
  (when-let [explanation (m/explain schema value)]
    (throw (ex-info (str prefixed-message (when prefixed-message ": ") (me/humanize explanation)) {}))))