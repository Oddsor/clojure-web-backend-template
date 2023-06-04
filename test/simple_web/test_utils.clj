(ns simple-web.test-utils
  (:require [malli.dev :as md]))

(defn instrumentation-fixture [f]
  (md/start!)
  (f)
  (md/stop!))