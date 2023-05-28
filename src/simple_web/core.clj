(ns simple-web.core
  (:gen-class))

(defn -main [& args]
  (println "Hello world! These are your args: " (pr-str args)))