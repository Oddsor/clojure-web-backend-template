(ns user
  (:require [clojure.repl.deps :refer [sync-deps]]
            [simple-web.core :as core]))

(comment
  ;; Evaluate this to add new dependencies while app is running
  (sync-deps)
  ;; Run the main method
  (core/-main 1 2 3))