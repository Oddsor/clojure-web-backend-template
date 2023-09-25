(ns user
  (:require [clojure.repl.deps :refer [sync-deps]]
            [flow-storm.api :as fs-api]
            [malli.dev :as md]
            [simple-web.core :as core]
            [juxt.clip.core :as clip]))

; Instrument and use function schemas while developing
(md/start!)

(def config (core/config "dev-config.edn"))

(comment
  ;; Evaluate this to add new dependencies while app is running
  (sync-deps)

  (fs-api/local-connect)

  (clip/require config)
  (def sys (clip/start config))
  (clip/stop config sys))