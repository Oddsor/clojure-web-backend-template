(ns simple-web.db
  (:require [next.jdbc :as jdbc]
            [honey.sql :as h]))

(with-open [conn (jdbc/get-connection {:jdbcUrl "jdbc:sqlite:sample.db"})]
  #_(jdbc/execute! conn (h/format {:create-table [:y :if-not-exists]
                                 :with-columns
                                 [[:id :uuid [:not nil]]
                                  [:name :text]
                                  [:money :integer]
                                  [[:primary-key :id]]]
                                 :raw "WITHOUT rowid"}))
  #_(jdbc/execute! conn
                 (h/format {:insert-into :y
                            :values [{:id (str (random-uuid))
                                      :name "Bob"
                                      :money 122}]}))
  (jdbc/with-transaction [x conn]
    (jdbc/execute! conn
                   (h/format {:insert-into :y
                              :values [{:id (str (random-uuid))
                                        :name "Bob"
                                        :money 125}]}))
    #_(throw (ex-info "Oops" {}))
    (jdbc/execute! conn
                   (h/format {:select :* :from :y}))))