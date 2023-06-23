(ns simple-web.db
  (:require [honey.sql :as h]
            [next.jdbc :as jdbc]
            [ragtime.core :as r]
            [ragtime.next-jdbc :as rn]
            [ragtime.strategy :as strategy]))

(def migrations
  (->> [{:id "create-account-table"
         :up (h/format {:create-table [:accounts :if-not-exists]
                        :with-columns
                        [[:id :uuid [:not nil]]
                         [:name :text]
                         [:money :integer]
                         [[:primary-key :id]]]
                        :raw "WITHOUT rowid"})
         :down (h/format {:drop-table :accounts})}
        {:id "create-user-table"
         :up (h/format {:create-table [:user :if-not-exists]
                        :with-columns
                        [[:id :uuid [:not nil]]
                         [:username :text :unique]
                         [:password :text]
                         [[:primary-key :id]]]
                        :raw "WITHOUT rowid"})
         :down (h/format {:drop-table :accounts})}]
       (map rn/map->SqlMigration)))

(comment
  (r/applied-migrations
   (rn/sql-database {:jdbcUrl "jdbc:sqlite:sample.db"})
   (r/into-index migrations))
  (r/migrate-all
   (rn/sql-database {:jdbcUrl "jdbc:sqlite:sample.db"})
   (r/into-index migrations)
   migrations
   {:strategy strategy/ignore-future})
  (jdbc/execute! (jdbc/get-connection {:jdbcUrl "jdbc:sqlite:sample.db"})
                 (h/format {:select :* :from :ragtime-migrations})))


(with-open [conn (jdbc/get-connection {:jdbcUrl "jdbc:sqlite:sample.db"})]
  #_(jdbc/execute! conn
                   (h/format {:insert-into :y
                              :values [{:id (str (random-uuid))
                                        :name "Bob"
                                        :money 122}]}))
  (jdbc/with-transaction [x conn]
    (jdbc/execute! x (h/format {:create-table [:y :if-not-exists]
                                :with-columns
                                [[:id :uuid [:not nil]]
                                 [:name :text]
                                 [:money :integer]
                                 [[:primary-key :id]]]
                                :raw "WITHOUT rowid"}))
    (jdbc/execute! x
                     (h/format {:insert-into :y
                                :values [{:id (str (random-uuid))
                                          :name "Bob"
                                          :money 125}]}))
    #_(throw (ex-info "Oops" {}))
    (jdbc/execute! x
                   (h/format {:select :* :from :y}))))

(defmacro safe-call [body & args]
  `(try
     ~body
     (catch Exception e#
       (if (contains? args :on-error)
         (println "Do nothing!")
         (throw e#)))))

(macroexpand (safe-call (/ 1 0) :on-error))