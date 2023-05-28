(ns user
  (:require [clojure.repl.deps :refer [sync-deps]]
            [integrant.core :as ig]
            [integrant.repl :as ir]
            [integrant.repl.state :as state]
            [malli.dev :as md]
            [simple-web.core :as core]))

; Instrument and use function schemas while developing
(md/start!)

(def config (core/config "dev-config.edn"))
(ig/load-namespaces (:system config))
(ir/set-prep! #(ig/prep (:system config)))

(comment
  ;; Evaluate this to add new dependencies while app is running
  (sync-deps)
  ;; Run the main method
  (core/-main 1 2 3)

  ; System state
  state/system
  ; (Re)start system
  (ir/reset)
  ; Stop system
  (ir/halt))