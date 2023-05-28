(ns simple-web.core-test
  (:require [clojure.test :refer [deftest is]]))

(deftest two-plus-two-equals-4 []
  (is (= (+ 2 2) 4)))