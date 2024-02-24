(ns todo-app.core
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]
            [juxt.clip.core :as clip]
            [dev.onionpancakes.chassis.core :as ch]
            [simple-web.base-router :as br]
            [todo-app.db :as db])
  (:gen-class))

(def root-input-spec [:map
                      [:name {:optional true} :any]])

(defn hidden-id-input [id]
  [:input {:name "id" :type "hidden" :value id}])

(defn page [title xs]
  (ch/html
   [:html {:lang "en"}
    [:head
     [:title title]
     [:link {:rel "stylesheet" :type "text/css" :href "main.css"}]
     [:script {:src "https://unpkg.com/htmx.org@1.9.2"}]
     [:meta {:charset "utf-8"}]]
    [:body {:hx-trigger "nytt-antall"
            :hx-get "/antall-oppgaver"
            :hx-target "#oppgave-teller"}
     xs]]))

(defn html [xs]
  (ch/html xs))

(def lag-oppgave-skjema
  [:form {:action "/lag-oppgave" :method "POST"
          :hx-boost "true" :hx-swap "outerHTML"
          :hx-target "closest form" :hx-push-url "false"}
   [:input {:type "text"
            :autofocus true
            :name "title"
            :placeholder "Legg til oppgave..."}]
   [:input {:type "submit" :value "Lag oppgave"}]])

(defn task-item
  ([{:keys [id title status] :as task} edit-task?]
   (let [li-id (str "todo-" id)
         target-id (str "#" li-id)
         can-edit? (not= "uredigerbar" title)]
     [:li {:id li-id :class "task"}
      (hidden-id-input id)
      (if edit-task?
        [:form {:hx-boost "true"
                :hx-push-url "false"
                :hx-target target-id
                :hx-swap "outerHTML"
                :action (str "/rediger-oppgave/" id)
                :method "POST"}
         [:input {:type "text"
                  :name "title"
                  :autofocus true
                  :on-focus "this.setSelectionRange(0, this.value.length)"
                  :value title}]]
        [:strong title])
      [:span.buttons
       (when can-edit?
         (if edit-task?
           [:a {:href "#" :hx-get (str "/hent-oppgave/" id) :hx-target target-id :hx-swap "outerHTML"} "↩️"]
           [:a {:href "#" :hx-get (str "/hent-oppgave/" id "?rediger=true") :hx-target target-id :hx-swap "outerHTML"} "📝"]))
       [:input (cond-> {:type "checkbox"
                        :hx-post "/godkjenn-oppgave"
                        :hx-include (str target-id " > [name='id']")
                        :name "status"
                        :hx-target "closest li"
                        :hx-swap "outerHTML"}
                 (= "DONE" status) (assoc :checked "checked"))]
       [:a {:href "#" :hx-post "/slett-oppgave" :hx-include (str target-id " > [name='id']") :hx-target "closest li" :hx-swap "outerHTML swap:0.5s"} "❌"]]]))
  ([task] (task-item task false)))

(defn task-list [tasks] (mapv task-item tasks))

(defn todo-body [tasks]
  [:body
   [:h1 "Todo-app (" [:span#oppgave-teller (count (filter (comp #{"NOT_DONE"} :status) tasks))] " gjenstår)"]
   [:div.ballcontainer [:div.ball]]
   [:p "Laget med " [:a {:href "https://htmx.org"} "htmx.org"]]
   lag-oppgave-skjema
   [:ul#oppgaveliste (task-list tasks)]])

(defn is-hx-request? [req]
  (-> req :headers (get "hx-request") (= "true")))

(defn get-task! [db id]
  (->> (db/get-tasks db)
       (map #(update-keys % (comp keyword name)))
       (filter #(= id (:id %)))
       first))
(defn get-tasks! [db]
  (map #(update-keys % (comp keyword name)) (db/get-tasks db)))

;; HTMX-trick for updating another part of the page:
;; Declare an event in the response header
(def nytt-antall-event-header {"Hx-Trigger" "nytt-antall"})
(def html-content-type-header {"Content-Type" "text/html; charset=utf-8"})

(def router
  [["/" {:get {:parameters {:query root-input-spec}
               :handler
               (fn [req]
                 (let [db (-> req :opts :db)
                       tasks (->> (get-tasks! db) (sort-by :date) reverse)]
                   (if (is-hx-request? req)
                     {:status 200
                      :headers html-content-type-header
                      :body (html (task-list tasks))}
                     {:status 200
                      :headers html-content-type-header
                      :body (page "Todo-app" (todo-body tasks))})))}}]
   ["/lag-oppgave" {:post {:parameters {:form [:map [:title :string]]}
                           :handler
                           (fn [req]
                             (let [db (-> req :opts :db)
                                   {:keys [title]} (-> req :parameters :form)
                                   new-id (db/create-task db title "")]
                               (if (is-hx-request? req)
                                 {:status 200
                                  :headers (merge html-content-type-header
                                                  nytt-antall-event-header)
                                  :body (html [;; HTMX trick for updating another part of the page:
                                               ;; Add a chunk of html that is inserted somewhere else
                                               [:div {:hx-swap-oob "afterbegin:#oppgaveliste"}
                                                (task-item (get-task! db (str new-id)))]
                                               lag-oppgave-skjema])}
                                 {:status 302
                                  :headers {"location" "/"}})))}}]
   ["/hent-oppgave/:id" {:get {:parameters {:path [:map [:id :string]]
                                            :query [:map [:rediger {:optional true} :string]]}
                               :handler
                               (fn [req]
                                 (let [db (-> req :opts :db)
                                       id (-> req :parameters :path :id)
                                       rediger? (-> req :parameters :query :rediger)]
                                   {:status 200
                                    :headers html-content-type-header
                                    :body (html (task-item (get-task! db id) (boolean rediger?)))}))}}]
   ["/rediger-oppgave/:id" {:post {:parameters {:path [:map [:id :string]]
                                                :form [:map [:title :string]]}
                                   :handler
                                   (fn [req]
                                     (let [id (-> req :parameters :path :id)
                                           title (-> req :parameters :form :title)
                                           db (-> req :opts :db)]
                                       (db/update-task db {:id id
                                                           :title title})
                                       {:status 200
                                        :headers html-content-type-header
                                        :body (html (task-item (get-task! db id)))}))}}]
   ["/godkjenn-oppgave" {:post {:parameters {:form [:map
                                                    [:id :string]
                                                    [:status {:optional true} :string]]}
                                :handler
                                (fn [req]
                                  (let [{:keys [status id]} (-> req :parameters :form)
                                        db (-> req :opts :db)]
                                    (db/update-task db {:id id
                                                        :status (if (= "on" status)
                                                                  "DONE"
                                                                  "NOT_DONE")})
                                    (if (is-hx-request? req)
                                      {:status 200
                                       :headers (merge html-content-type-header
                                                       nytt-antall-event-header)
                                       :body (html (task-item (get-task! db id)))}
                                      {:status 302
                                       :headers {"location" "/"}})))}}]
   ["/slett-oppgave" {:post {:parameters {:form [:map
                                                 [:id :string]]}
                             :handler
                             (fn [req]
                               (let [task-id (-> req :parameters :form :id)
                                     db (-> req :opts :db)]
                                 (db/delete-task db task-id)
                                 (if (is-hx-request? req)
                                   {:status 200
                                    :headers (merge html-content-type-header
                                                    nytt-antall-event-header)
                                    :body ""}
                                   {:status 302
                                    :headers {"location" "/"}})))}}]
   ["/antall-oppgaver" {:get (fn [req]
                               (let [db (-> req :opts :db)
                                     tasks (get-tasks! db)]
                                 {:status 200
                                  :headers html-content-type-header
                                  :body (str (count (filter #(= "NOT_DONE" (:status %)) tasks)))}))}]])

(defn handler [opts]
  (if (:dev opts)
    ;; This sneaky layer of indirection will ensure that while developing, the
    ;; router will correctly reload when loading the namespace.
    ;; For production, this should not be done as it causes the app to perform
    ;; unnecessary work.
    (fn [req] ((br/base-handler router opts) req))
    (br/base-handler router opts)))

(defn config [filename]
  (read-config (io/resource filename)))

(defn -main [& _args]
  (let [config (config "todo-config.edn")]
    (clip/start config)
    @(future)))