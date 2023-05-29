(ns simple-web.logging
  (:require [com.brunobonacci.mulog :as u]
            [integrant.core :as ig]))

(defmethod ig/init-key ::console [_ _opts]
  (u/start-publisher! {:type :console}))

(defmethod ig/halt-key! ::console [_ publisher]
  (publisher))