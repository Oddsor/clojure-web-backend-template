(ns user
  (:require [clojure.repl.deps :refer [sync-deps]]
            [flow-storm.api :as fs-api]
            [malli.dev :as md]
            [simple-web.utils :as utils]
            [juxt.clip.repl :as crepl]))

; Instrument and use function schemas while developing
(md/start!)

(crepl/set-init! #(utils/config "dev-config.edn"))

(comment
  ;; Evaluate this to add new dependencies while app is running
  (sync-deps)

  (fs-api/local-connect)

  (crepl/reset)
  (crepl/stop))