(ns todomvc.commands
  (:require [clojure.spec.alpha :as s]
            [datascript.core :as d]
            [mook.core :as m]
            [orchestra.core :refer-macros [defn-spec]]
            [promesa.core :as p]
            [todomvc.boundaries.ui :as b-ui]
            [todomvc.boundaries.todo :as b-todo]
            [todomvc.stores :as stores]))

(defn-spec create-new-todo>> p/promise?
  {:private true}
  [data (s/merge ::stores/stores*
                 (s/keys :req [:todo/title]))]
  (d/transact! (::stores/app-db* data)
               [(merge (select-keys data [:todo/title])
                       {:todo/completed? false
                        :todo/created-at (js/Date.)})])
  (p/resolved (dissoc data :todo/title)))

(def <create-new-todo>>
  (m/wrap create-new-todo>>))

;; ---

(defn-spec toggle-todo-status>> p/promise?
  {:private true}
  [data (s/merge ::stores/stores*
                 (s/keys :req [:db/id]))]
  (let [app-db* (::stores/app-db* data)]
    (when-let [{:todo/keys [completed?]} (d/pull @app-db* [:todo/completed?] (:db/id data))]
      (d/transact! app-db* [(merge (select-keys data [:db/id])
                                   {:todo/completed? (not completed?)})])))
  (p/resolved (dissoc data :db/id)))

(def <toggle-todo-status>>
  (m/wrap toggle-todo-status>>))

;; ---

(defn-spec destroy-todo>> p/promise?
  {:private true}
  [data (s/merge ::stores/stores*
                 (s/keys :req [:db/id]))]
  (d/transact! (::stores/app-db* data)
               [[:db.fn/retractEntity (:db/id data)]])
  (p/resolved (dissoc data :db/id)))

(def <destroy-todo>>
  (m/wrap destroy-todo>>))

;; ---

(defn-spec clear-completed-todos>> p/promise?
  {:private true}
  [data ::stores/stores*]
  ;; ---
  (let [app-db* (::stores/app-db* data)]
    (as-> @app-db* <>
      (d/q '[:find [?e ...]
             :where [?e :todo/completed? true]]
           <>)
      (mapv #(do [:db.fn/retractEntity %]) <>)
      (d/transact! app-db* <>)))
  (p/resolved data))

(defn <clear-completed-todos>> []
  (as-> (m/wrap clear-completed-todos>>) <>
    (<> {})))

;; ---

(defn-spec toggle-all>> p/promise?
  {:private true}
  [data (s/merge ::stores/stores*
                 (s/keys :req [::b-ui/all-completed?]))]
  (let [app-db* (::stores/app-db* data)
        all-completed? (::b-ui/all-completed? data)]
    (as-> @app-db* <>
      (d/q '[:find [?e ...]
             :where [?e :todo/completed?]]
           <>)
      (mapv #(do {:db/id %
                  :todo/completed? (not all-completed?)})
            <>)
      (d/transact! app-db* <>)))
  (p/resolved (dissoc data ::b-ui/all-completed?)))

(def <toggle-all>>
  (m/wrap toggle-all>>))

;; ---

(defn-spec set-filter>> p/promise?
  {:private true}
  [data (s/merge ::stores/stores*
                 (s/keys :req [::b-ui/active-filter]))]
  (swap! (::stores/local-store* data) merge (select-keys data [::b-ui/active-filter]))
  (p/resolved (dissoc data ::b-ui/active-filter)))

(def <set-filter>>
  (m/wrap set-filter>>))

;; ---

(defn-spec update-todo>> p/promise?
  {:private true}
  [data (s/merge ::stores/stores*
                 (s/keys :req [:db/id :todo/title]))]
  (d/transact! (::stores/app-db* data) [(select-keys data [:db/id :todo/title])])
  (p/resolved (dissoc data :db/id :todo/title)))

(def <update-todo>>
  (m/wrap update-todo>>))
