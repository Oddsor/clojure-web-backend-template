(ns simple-web.server-test
  (:require [clojure.test :refer [deftest is]]
            [integrant.core :as ig]
            [hato.client :as h]))

(deftest server-test
  (let [response {:status 200
                  :body "ok!"}
        config {:simple-web.server/server {:port 12345
                                           :handler (fn [_req]
                                                      response)}}
        _ (ig/load-namespaces config)
        system (ig/init config)]
    (is (= response
           (-> (h/get "http://localhost:12345/")
               (select-keys (keys response)))))
    (ig/halt! system)))