(ns simple-web.db
  (:require [honey.sql :as h]
            [next.jdbc :as jdbc]))

(def migrations
  [(let [table-name :task]
     {:id "create-task-table"
      :up {:create-table [table-name]
           :with-columns
           [[:id :uuid [:not nil]]
            [:title :text]
            [:description :text]
            [:date :datetime]
            [:status :text]
            [[:primary-key :id]]]
           :raw "WITHOUT rowid"}
      :down {:drop-table table-name}})])

(defn execute! [conn sql]
  (jdbc/execute! conn (h/format sql)))

(defn apply-migrations [db-spec migrations]
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
    (println applied-migrations)
    (doseq [migration migrations
            :when (not (applied-ids (:id migration)))]
      (println (format "Applying migration: %s" (:id migration)))
      (execute! conn (:up migration))
      (execute! conn {:insert-into :migrations
                      :values [{:id (:id migration)
                                :date (java.time.Instant/now)}]}))))

(comment
  (apply-migrations {:jdbcUrl "jdbc:sqlite:sample.db"} migrations))

(defprotocol TaskDb
  (get-tasks [this] "Get tasks")
  (delete-task [this id] "Delete task by id")
  (update-task [this task] "Update task")
  (create-task [this id title description] "Create task"))

(defrecord Conny [conn]
  TaskDb
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
  (create-task [this id title description]
    (execute! conn {:insert-into :task
                    :values [{:id id
                              :title title
                              :description description
                              :status "NOT_DONE"
                              :date (java.time.Instant/now)}]})))

(comment
  (let [db (->Conny (jdbc/get-connection {:jdbcUrl "jdbc:sqlite:sample.db"}))]
    (get-tasks db)
    #_(delete-task db "288a884a-99b1-4648-bdbf-0c19c33473d8") 
    (create-task db "Hei!" "Dette er en oppgave som må gjøres")))

(comment
  (defmacro safe-call [& args]
    (println args)
    (let [[body on-error-xs args] (partition-by #{:on-error} args)
          on-error (first on-error-xs)]
      `(try
         ~@body
         (catch Exception e#
           (println ~args)
           (if (some? ~on-error)
             (println "Do nothing!")
             (throw e#))))))


  (macroexpand (safe-call (/ 1 0) :on-error)))