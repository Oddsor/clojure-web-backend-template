(ns user
  (:require [flow-storm.api :as fs-api]
            [malli.dev :as md]
            [simple-web.core :as core]
            [juxt.clip.core :as clip]))

; Instrument and use function schemas while developing
(md/start!)

(def config (core/config "todo-dev-config.edn"))

(comment
  (fs-api/local-connect)

  (clip/require config)
  (def sys (clip/start config))
  (clip/stop config sys))