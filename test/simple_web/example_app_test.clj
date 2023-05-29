(ns simple-web.example-app-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [simple-web.example-app :as ea]
            [simple-web.utils :as utils]))

(use-fixtures :once utils/instrumentation-fixture)

(deftest app-routing-test
  (is (= "Hello, world!"
         (-> (ea/dev-handler {:request-method :get
                              :uri "/"})
             :body))
      "No parameter input")

  (is (= "Hello, test!"
         (-> (ea/dev-handler {:request-method :get
                              :uri "/test"})
             :body))
      "Path-based parameters")

  (is (= "Hello, test!"
         (-> (ea/dev-handler {:request-method :post
                              :uri "/"
                              :body-params {:name "test"}})
             :body))
      "Body-based parameters")

  (is (= "Hello, test!"
         (-> (ea/dev-handler {:request-method :post
                              :content-type "application/x-www-form-urlencoded"
                              :uri "/"
                              :form-params {:name "test"}})
             :body))
      "Form-based parameters")

  (is (= "Hello, test!"
         (-> (ea/dev-handler {:request-method :get
                              :uri "/"
                              :query-params {:name "test"}})
             :body))
      "Query-based parameters"))