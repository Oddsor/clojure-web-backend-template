(ns simple-web.server
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty])
  (:import [org.eclipse.jetty.server Server]))

(defmethod ig/init-key ::server [_ {:keys [handler port]}]
  (jetty/run-jetty handler {:port port
                            :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (.stop ^Server server))