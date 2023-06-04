(ns simple-web.utils 
  (:require [malli.core :as m]
            [malli.error :as me]))

(defn assert-schema [schema value prefixed-message]
  (when-let [explanation (m/explain schema value)]
    (throw (ex-info (str prefixed-message (when prefixed-message ": ") (me/humanize explanation)) {}))))