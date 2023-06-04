(ns simple-web.core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [simple-web.core :as core]
            [simple-web.test-utils :as utils]))

(use-fixtures :once utils/instrumentation-fixture)

(deftest not-correct-input-for-system
  (is (thrown? AssertionError
               (core/init-system nil))
      "Not passing a valid argument to an instrumented function will
       result in an AssertionError (nb! only instrumented during dev and test)"))