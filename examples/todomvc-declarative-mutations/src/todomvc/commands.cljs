(ns todomvc.commands
  (:require [clojure.spec.alpha :as s]
            [datascript.core :as d]
            [mook.core :as m]
            [mook.core :as m]
            [orchestra.core :refer-macros [defn-spec]]
            [promesa.core :as p]
            [todomvc.boundaries.todo :as b-todo]
            [todomvc.boundaries.ui :as b-ui]
            [todomvc.stores :as stores]))

(defn-spec create-new-todo>> p/promise?
  {:private true}
  [data (s/merge ::stores/states
                 (s/keys :req [:todo/title]))]
  (as-> data <>
    (d/db-with (::stores/app-db <>)
               [(merge (select-keys data [:todo/title])
                       {:todo/completed? false
                        :todo/created-at (js/Date.)})])
    (p/resolved
      (-> data
          (dissoc :todo/title)
          (assoc ::m/state-transitions [{::m/state-key ::stores/app-db
                                         ::m/new-state <>}])))))

(def <create-new-todo>>
  (m/wrap create-new-todo>>))

;; ---

(defn-spec toggle-todo-status>> p/promise?
  {:private true}
  [data (s/merge ::stores/states
                 (s/keys :req [:db/id]))]
  (let [app-db (::stores/app-db data)]
    (as-> data <>
      (if-let [{:todo/keys [completed?]} (d/pull app-db [:todo/completed?] (:db/id <>))]
        {::m/state-transitions [{::m/state-key ::stores/app-db
                                 ::m/new-state (d/db-with app-db
                                                          [(merge (select-keys <> [:db/id])
                                                                  {:todo/completed? (not completed?)})])}]}
        {})
      (p/resolved
        (-> data
            (dissoc :db/id)
            (merge <>))))))

(def <toggle-todo-status>>
  (m/wrap toggle-todo-status>>))

;; ---

(defn-spec destroy-todo>> p/promise?
  {:private true}
  [data (s/merge ::stores/states
                 (s/keys :req [:db/id]))]
  (as-> data <>
    (d/db-with (::stores/app-db <>)
               [[:db.fn/retractEntity (:db/id <>)]])
    (p/resolved
      (-> data
          (dissoc :db/id)
          (assoc ::m/state-transitions [{::m/state-key ::stores/app-db
                                         ::m/new-state <>}])))))

(def <destroy-todo>>
  (m/wrap destroy-todo>>))

;; ---

(defn-spec clear-completed-todos>> p/promise?
  {:private true}
  [data ::stores/states]
  ;; ---
  (let [app-db (::stores/app-db data)]
    (as-> app-db <>
      (d/q '[:find [?e ...]
             :where [?e :todo/completed? true]]
           <>)
      (mapv #(do [:db.fn/retractEntity %]) <>)
      (d/db-with app-db <>)
      (p/resolved
        (assoc data
               ::m/state-transitions
               [{::m/state-key ::stores/app-db
                 ::m/new-state <>}])))))

(defn <clear-completed-todos>> []
  (as-> (m/wrap clear-completed-todos>>) <>
    (<> {})))

;; ---

(defn-spec toggle-all>> p/promise?
  {:private true}
  [data (s/merge ::stores/states
                 (s/keys :req [::b-ui/all-completed?]))]
  (let [app-db (::stores/app-db data)
        all-completed? (::b-ui/all-completed? data)]
    (as-> app-db <>
      (d/q '[:find [?e ...]
             :where [?e :todo/completed?]]
           <>)
      (mapv #(do {:db/id %
                  :todo/completed? (not all-completed?)})
            <>)
      (d/db-with app-db <>)
      (p/resolved
        (-> data
            (dissoc ::b-ui/all-completed?)
            (assoc ::m/state-transitions
                   [{::m/state-key ::stores/app-db
                     ::m/new-state <>}]))))))

(def <toggle-all>>
  (m/wrap toggle-all>>))

;; ---

(defn-spec set-filter>> p/promise?
  {:private true}
  [data (s/merge ::stores/states
                 (s/keys :req [::b-ui/active-filter]))]
  (as-> (::stores/local-store data) <>
    (merge <> (select-keys data [::b-ui/active-filter]))
    (p/resolved
      (-> data
          (dissoc ::b-ui/active-filter)
          (assoc ::m/state-transitions
                 [{::m/state-key ::stores/local-store
                   ::m/new-state <>}])))))

(def <set-filter>>
  (m/wrap set-filter>>))

;; ---

(defn-spec update-todo>> p/promise?
  {:private true}
  [data (s/merge ::stores/states
                 (s/keys :req [:db/id :todo/title]))]
  (as-> (::stores/app-db data) <>
    (d/db-with <> [(select-keys data [:db/id :todo/title])])
    (p/resolved
      (-> data
          (dissoc :db/id :todo/title)
          (assoc ::m/state-transitions
                 [{::m/state-key ::stores/app-db
                   ::m/new-state <>}])))))

(def <update-todo>>
  (m/wrap update-todo>>))
