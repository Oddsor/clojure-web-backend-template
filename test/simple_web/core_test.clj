(ns simple-web.core-test
  (:require [clojure.test :refer [deftest is]]
            [simple-web.core :as core]))

(deftest system-correctly-initiates []
  (is (= "Correct message:\"ok\""
         (-> (core/init-system {::core/handler {:msg "Correct message:"}})
             ::core/handler
             (apply ["ok"])))))