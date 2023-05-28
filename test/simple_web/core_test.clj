(ns simple-web.core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [simple-web.core :as core]
            [simple-web.utils :as utils]))

(use-fixtures :once utils/instrumentation-fixture)

(deftest system-correctly-initiates []
  (is (= "Correct message:\"ok\""
         (-> (core/init-system {::core/handler {:msg "Correct message:"}})
             ::core/handler
             (apply ["ok"])))))

(deftest not-correct-input-for-system []
  (is (thrown? AssertionError
         (-> (core/init-system nil)
             ::core/handler
             (apply ["ok"])))
      "Not passing a valid argument to an instrumented function will
       result in an AssertionError (nb! only instrumented during dev and test)"))