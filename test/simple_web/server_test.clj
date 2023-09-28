(ns simple-web.server-test
  (:require [clojure.test :refer [deftest is]]
            [juxt.clip.core :as clip]
            [hato.client :as h]))

(deftest server-test
  (let [response {:status 200
                  :body "ok!"}
        config {:components
                {:server {:start `(ring.adapter.jetty/run-jetty
                                   (constantly
                                     ~response)
                                   {:port 12345
                                    :join? false})}}}
        _ (clip/require config)]
    (clip/with-system [system config]
      (println system config)
      (is (= response
             (-> (h/get "http://localhost:12345/")
                 (select-keys (keys response))))))))