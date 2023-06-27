(ns simple-web.todo-app
  (:require [integrant.core :as ig]
            [rum.core :as rum]
            [selmer.parser :as selmer]
            [simple-web.auth :as auth]
            [simple-web.base-router :as br]
            [simple-web.db :as db]
            [com.brunobonacci.mulog :as mu]
            [next.jdbc :as jdbc]
            [jsonista.core :as json]))

(def root-input-spec [:map
                      [:name {:optional true} :any]])

(defn page [title xs]
  (rum/render-static-markup
   [:html {:lang "en"}
    [:head
     [:title title]
     [:link {:rel "stylesheet" :type "text/css" :href "main.css"}]
     [:script {:src "https://unpkg.com/htmx.org@1.9.2"}]
     [:meta {:charset "utf-8"}]]
    [:body {:hx-trigger "count-updated"
            :hx-get "/task-count"
            :hx-target "#task-count"}
     xs]]))

(defn html [xs]
  (rum/render-static-markup xs))

(def form
  [:form {:action "/lag-oppgave" :method "POST"
          :hx-boost "true" :hx-swap "outerHTML"
          :hx-target "closest form" :hx-push-url "false"}
   [:h2 "Ny oppgave"]
   [:label {:for "titl"} "Tittel"]
   [:input#titl {:type "text" :name "title"}]
   [:br]
   [:label {:for "desc"} "Beskrivelse"]
   [:textarea#desc {:name "description"}]
   [:br]
   [:input {:type "submit" :value "Lag oppgave"}]])

(defn task-item [task]
  (let [id (str "todo-" (:id task))
        target-id (str "#" id)]
    [:li {:id id :class "task"}
     [:span.tasktitle
      [:strong (:title task)]
      [:span.buttons
       [:form {:action "/godkjenn-oppgave" :method "POST"
               :hx-boost "true" :hx-push-url "false"
               :hx-target target-id :hx-swap "outerHTML"}
        [:input {:name "id" :type "hidden" :value (:id task)}]
        [:input {:name "status" :type "hidden" :value (if (= "DONE" (:status task)) "NOT_DONE" "DONE")}]
        [:input.check {:type "submit" :value (if (= "DONE" (:status task))
                                               "✔️" " ")}]]
       [:form {:action "/slett-oppgave" :method "POST"
               :hx-target target-id :hx-swap "outerHTML swap:1s"
               :hx-boost "true" :hx-push-url "false"}
        [:input {:type "hidden" :name "id" :value (:id task)}]
        [:input.del {:type "submit" :value "❌"}]]]]
     [:span (:description task)]]))

(defn task-list [tasks]
  [:<> (map task-item tasks)])

(defn todo-body [tasks]
  [:body
   [:h1 "Todlido! (" [:span#task-count (count (filter (comp #{"NOT_DONE"} :status) tasks))] " gjenstår)"]
   [:textarea]
   [:article {:hx-trigger "tasklist-updated" :hx-get "/" :hx-target "#task-list"}
    form
    [:ul#task-list (task-list tasks)]]])

(defn is-hx-request? [req]
  (-> req :headers (get "hx-request") (= "true")))

(defn get-tasks [db]
  (map #(update-keys % (comp keyword name)) (db/get-tasks db)))

(def router
  [["/" {:get {:parameters {:query root-input-spec}
               :handler
               (fn [req]
                 (let [db (-> req :opts :db)
                       tasks (->> (get-tasks db) (sort-by :date) reverse)]
                   (if (is-hx-request? req)
                     {:status 200
                      :body (html (task-list tasks))}
                     {:status 200
                      :body (page "Todlido" (todo-body tasks))})))}}]
   ["/lag-oppgave" {:post {:parameters {:form [:map
                                               [:title :string]
                                               [:description :string]]}
                           :handler
                           (fn [req]
                             (let [db (-> req :opts :db)
                                   {:keys [title description]} (-> req :parameters :form)]
                               (db/create-task db title description)
                               (if (is-hx-request? req)
                                 {:status 200
                                  :headers {"Hx-Trigger" (json/write-value-as-string {"count-updated" "" "tasklist-updated" ""})}
                                  :body (html form)}
                                 {:status 302
                                  :headers {"location" "/"}})))}}]
   ["/godkjenn-oppgave" {:post {:parameters {:form [:map
                                                    [:id :string]
                                                    [:status {:optional true} :string]]}
                                :handler
                                (fn [req]
                                  (let [{:keys [status id]} (-> req :parameters :form)
                                        db (-> req :opts :db)]
                                    (db/update-task db {:id id
                                                        :status (if (= "DONE" status)
                                                                  "DONE"
                                                                  "NOT_DONE")})
                                    (let [tasks (get-tasks db)
                                          task (first (filter #(= id (:id %)) tasks))]
                                      (if (is-hx-request? req)
                                        {:status 200
                                         :headers {"Hx-Trigger" "count-updated"}
                                         :body (html (task-item task))}
                                        {:status 302
                                         :headers {"location" "/"}}))))}}]
   ["/slett-oppgave" {:post {:parameters {:form [:map
                                                 [:id :string]]}
                             :handler
                             (fn [req]
                               (let [task-id (-> req :parameters :form :id)
                                     db (-> req :opts :db)]
                                 (db/delete-task db task-id)
                                 (if (is-hx-request? req)
                                   {:status 200
                                    :headers {"Hx-Trigger" "count-updated"}
                                    :body ""}
                                   {:status 302
                                    :headers {"location" "/"}})))}}]
   ["/task-count" {:get (fn [req]
                          (let [db (-> req :opts :db)
                                tasks (get-tasks db)]
                            {:status 200
                             :body (str (count (filter #(= "NOT_DONE" (:status %)) tasks)))}))}]])

(defn dev-handler
  "This sneaky layer of indirection will ensure that while developing, the
   router will correctly reload when loading the namespace.
   
   For production, this should not be done as it causes the app to perform
   unnecessary work."
  [opts req]
  (let [db (db/->Conny (jdbc/get-connection {:jdbcUrl "jdbc:sqlite:sample.db"}))]
    ((br/handler router (assoc opts :db db)) req)))

(defmethod ig/init-key ::handler [_ {:keys [dev] :as opts}]
  (if dev
    (partial dev-handler opts)
    (br/handler router opts)))