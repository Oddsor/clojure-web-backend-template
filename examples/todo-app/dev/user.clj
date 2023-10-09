(ns user
  (:require [flow-storm.api :as fs-api]
            [juxt.clip.repl :as crepl]
            [malli.dev :as md]
            [simple-web.utils :as utils]))

; Instrument and use function schemas while developing
(md/start!)

(crepl/set-init! #(utils/config "todo-dev-config.edn"))

(comment
  (fs-api/local-connect)

  (crepl/reset)
  (crepl/stop))