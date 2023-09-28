(ns simple-web.example-app-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [simple-web.example-app :as ea]
            [simple-web.test-utils :as utils]))

(use-fixtures :once utils/instrumentation-fixture)

(def handler (ea/handler {}))

(deftest app-routing-test
  (is (= "Hello, world!"
         (-> (handler {:request-method :get
                          :uri "/"})
             :body))
      "No parameter input")

  (is (= "Hello, test!"
         (-> (handler {:request-method :get
                          :uri "/path/test"})
             :body))
      "Path-based parameters")

  (is (= "Hello, test!"
         (-> (handler {:request-method :post
                          :uri "/"
                          :body-params {:name "test"}})
             :body))
      "Body-based parameters")

  (is (= "Hello, test!"
         (-> (handler {:request-method :post
                          :content-type "application/x-www-form-urlencoded"
                          :uri "/"
                          :form-params {:name "test"}})
             :body))
      "Form-based parameters")

  (is (= "Hello, test!"
         (-> (handler {:request-method :get
                          :uri "/"
                          :query-params {:name "test"}})
             :body))
      "Query-based parameters"))