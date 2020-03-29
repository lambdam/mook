(ns todomvc.commands
  (:require [cljs.spec.alpha :as s]
            [datascript.core :as d]
            [mook.core :as c]
            [orchestra.core :refer-macros [defn-spec]]
            [promesa.core :as p]
            todomvc.specs ;; don't ns-clean
            ))

(defn-spec create-new-todo>> :action/promise
  [{:todomvc.store/keys [app-db*]
    :todo/keys [title]
    :as data}
   (s/keys :req [:todomvc.store/app-db* :todo/title])]
  ;; ---
  (d/transact! app-db* [{:todo/title title
                         :todo/completed? false
                         :todo/created-at (js/Date.)}])
  (p/resolved (dissoc data :component/keys)))

;; ---

(defn-spec toggle-todo-status>> :action/promise
  [{:todomvc.store/keys [app-db*]
    :db/keys [id]
    :as data}
   (s/keys :req [:todomvc.store/app-db* :db/id])]
  ;; ---
  (when-let [{:todo/keys [completed?]} (d/pull @app-db* [:todo/completed?] id)]
    (d/transact! app-db* [{:db/id id
                           :todo/completed? (not completed?)}]))
  (p/resolved (dissoc data :db/id)))

;; ---

(defn-spec destroy-todo>> :action/promise
  [{:todomvc.store/keys [app-db*]
    :db/keys [id]
    :as data}
   (s/keys :req [:todomvc.store/app-db* :db/id])]
  ;; ---
  (d/transact! app-db* [[:db.fn/retractEntity id]])
  (p/resolved (dissoc data :db/id)))

;; ---

(defn-spec clear-completed-todos>> :action/promise
  [{:todomvc.store/keys [app-db*]
    :as data}
   (s/keys :req [:todomvc.store/app-db*])]
  ;; ---
  (as-> @app-db* <>
    (d/q '[:find [?e ...]
           :where [?e :todo/completed? true]]
         <>)
    (mapv #(do [:db.fn/retractEntity %]) <>)
    (d/transact! app-db* <>))
  (p/resolved data))

;; ---

(defn-spec toggle-all>> :action/promise
  [{:todomvc.store/keys [app-db*]
    :component/keys [all-completed?]
    :as data}
   (s/keys :req [:todomvc.store/app-db* :component/all-completed?])]
  ;; ---
  (as-> @app-db* <>
    (d/q '[:find [?e ...]
           :where [?e :todo/completed?]]
         <>)
    (mapv #(do {:db/id %
                :todo/completed? (not all-completed?)})
          <>)
    (d/transact! app-db* <>))
  (p/resolved (dissoc data :component/all-completed?)))

;; ---

(defn-spec set-filter>> :action/promise
  [{:todomvc.store/keys [local-store*]
    :local-store/keys [active-filter]
    :as data}
   (s/keys :req [:todomvc.store/local-store* :local-store/active-filter])]
  ;; ---
  (swap! local-store* update :local-store/active-filter #(do active-filter))
  (p/resolved (dissoc data :local-store/active-filter)))

;; ---

(defn-spec update-todo>> :action/promise
  [{:todomvc.store/keys [app-db*]
    :db/keys [id]
    :todo/keys [title]
    :as data}
   (s/keys :req [:todomvc.store/app-db* :db/id :todo/title])]
  ;; ---
  (d/transact! app-db* [{:db/id id
                         :todo/title title}])
  (p/resolved (dissoc data :todo/id :todo/title)))
