(ns build
  (:require [clojure.tools.build.api :as b]))
(def app-name "todo-app")
(def version "1.0.0")

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar"
                       app-name version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  ;; Direct linking
  (binding [*compiler-options* {:direct-linking true}]
    (clean nil)
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    :src-dirs ["src"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main 'todo-app.core})))
