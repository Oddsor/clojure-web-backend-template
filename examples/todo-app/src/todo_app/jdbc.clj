(ns todo-app.jdbc
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.brunobonacci.mulog :as mu]
            [honey.sql :as h]
            [next.jdbc :as jdbc]
            [todo-app.db :as todo-db])
  (:import [java.io PushbackReader]
           [java.time Instant]))

(defn execute! [conn sql]
  (jdbc/execute! conn (h/format sql)))

(defn- apply-migrations [db-spec migrations]
  (let [conn (jdbc/get-connection db-spec)
        _ (execute! conn {:create-table [:migrations :if-not-exists]
                          :with-columns
                          [[:id :text :unique :primary-key]
                           [:date :date [:not nil]]]
                          :raw "WITHOUT rowid"})
        applied-migrations (execute! conn {:select :*
                                           :from :migrations
                                           :order-by :date})
        applied-ids (->> applied-migrations (map :migrations/id) set)]
    (mu/log ::migrations-found :migrations applied-ids)
    (doseq [{migration-id :id :as migration} migrations
            :when (not (applied-ids (:id migration)))]
      (mu/log ::apply-migration :migration-id migration-id :messsage (format "Applying migration: %s" migration-id))
      (execute! conn (:up migration))
      (execute! conn {:insert-into :migrations
                      :values [{:id (:id migration)
                                :date (java.time.Instant/now)}]}))))

(defn perform-migrations! [db-spec migration-path]
  (apply-migrations
   db-spec
   (mapv (comp edn/read-string slurp fs/file)
         (or (seq (fs/glob migration-path "**.edn"))
             (fs/glob "resources" (str (fs/path migration-path "**.edn")))))))

(comment
  (perform-migrations! {:jdbcUrl "jdbc:sqlite:sample.db"} "migrations"))

(deftype JdbcConnection [conn]
  todo-db/TaskDb
  (get-tasks [this]
    (execute! conn {:select :*
                    :from :task}))
  (delete-task [this id]
    (execute! conn {:delete-from :task
                    :where [:= :id id]}))
  (update-task [this task]
    (let [id (:id task)
          task-fields (select-keys task [:title :description :status])]
      (assert (some? id) "Task must have id!")
      (execute! conn {:update :task
                      :set task-fields
                      :where [:= :id id]})))
  (create-task [this title description]
    (let [id (random-uuid)]
      (execute! conn {:insert-into :task
                      :values [{:id id
                                :title title
                                :description description
                                :status "NOT_DONE"
                                :date (Instant/now)}]})
      id)))

(defn make-jdbc-connection [jdbc-url]
  (->JdbcConnection (jdbc/get-connection {:jdbcUrl jdbc-url})))

(comment
  (let [db (make-jdbc-connection "jdbc:sqlite:sample.db")]
    (println (todo-db/get-tasks db))
    #_(delete-task db "288a884a-99b1-4648-bdbf-0c19c33473d8")
    (todo-db/create-task db "Hei!" "Dette er en oppgave som må gjøres")))
