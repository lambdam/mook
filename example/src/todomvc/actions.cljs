(ns todomvc.actions
  (:require [cljs.spec.alpha :as s]
            [promesa.core :as p]
            [orchestra.core :refer-macros [defn-spec]]
            todomvc.specs ;; don't ns-clean
            ))

(defn-spec ^:private create-new-todo :state/todos
  [todos :state/todos
   title string?]
  ;; ---
  (into [#:entity.todo{:id (str (random-uuid))
                       :title title
                       :completed? false}]
        todos))

(defn-spec create-new-todo>> :action/promise
  [{:react-context/keys [app-state*]
    :component/keys [title]
    :as data}
   (s/keys :req [:react-context/app-state* :component/title])]
  ;; ---
  (swap! app-state* update :state/todos create-new-todo title)
  (p/resolved (dissoc data :component/keys)))

;; ---

(defn-spec ^:private toggle-todo-status :react-context/app-state
  [app-state :react-context/app-state
   id :entity.todo/id]
  ;; ---
  (if-let [[index todo] (reduce-kv (fn [acc k v]
                                     (if (= id (:entity.todo/id v))
                                       (reduced [k v])
                                       acc))
                                   nil
                                   (:state/todos app-state))]
    (update-in app-state
               [:state/todos index :entity.todo/completed?]
               not)
    app-state))

(defn-spec toggle-todo-status>> :action/promise
  [{:react-context/keys [app-state*]
    :entity.todo/keys [id]
    :as data}
   (s/keys :req [:react-context/app-state* :entity.todo/id])]
  ;; ---
  (swap! app-state* #(toggle-todo-status % id))
  (p/resolved (dissoc data :entity.todo/id)))

;; ---

(defn-spec ^:private destroy-todo :state/todos
  [todos :state/todos
   id :entity.todo/id]
  (filterv #(not= (:entity.todo/id %) id)
           todos))

(defn-spec destroy-todo>> :action/promise
  [{:react-context/keys [app-state*]
    :entity.todo/keys [id]
    :as data}
   (s/keys :req [:react-context/app-state* :entity.todo/id])]
  ;; ---
  (swap! app-state* update :state/todos destroy-todo id)
  (p/resolved (dissoc data :entity.todo/id)))

;; ---

;; TODO: does (fn-spec ...) exists so that it is not
;; mandatory to split the async function?
(defn-spec ^:private clear-completed-todos :state/todos
  [todos :state/todos]
  (filterv #(false? (:entity.todo/completed? %)) todos))

(defn-spec clear-completed-todos>> :action/promise
  [{:react-context/keys [app-state*]
    :as data}
   (s/keys :req [:react-context/app-state*])]
  ;; ---
  (swap! app-state* update :state/todos clear-completed-todos)
  (p/resolved data))

;; ---

(defn-spec ^:private toggle-all :state/todos
  [todos :state/todos
   all-completed? :component/all-completed?]
  ;; ---
  (mapv #(assoc % :entity.todo/completed? (not all-completed?))
        todos))

(defn-spec toggle-all>> :action/promise
  [{:react-context/keys [app-state*]
    :component/keys [all-completed?]
    :as data}
   (s/keys :req [:react-context/app-state* :component/all-completed?])]
  ;; ---
  (swap! app-state* update :state/todos toggle-all all-completed?)
  (p/resolved (dissoc data :component/all-completed?)))

;; ---

(defn-spec set-filter>> :action/promise
  [{:react-context/keys [app-state*]
    :state.local/keys [active-filter]
    :as data}
   (s/keys :req [:react-context/app-state* :state.local/active-filter])]
  ;; ---
  (swap! app-state* update :state.local/active-filter #(do active-filter))
  (p/resolved (dissoc data :state.local/active-filter)))

;; ---

(defn-spec ^:private update-todo :state/todos
  [todos :state/todos
   id :entity.todo/id
   title :entity.todo/title]
  ;; ---
  (if-let [[index todo] (reduce-kv (fn [acc k v]
                                     (if (= id (:entity.todo/id v))
                                       (reduced [k v])
                                       acc))
                                   nil
                                   todos)]
    (update-in todos
               [index :entity.todo/title]
               #(do title))
    todos))

(defn-spec update-todo>> :action/promise
  [{:react-context/keys [app-state*]
    :entity.todo/keys [id]
    :component/keys [title]
    :as data}
   (s/keys :req [:react-context/app-state* :component/title :entity.todo/id])]
  ;; ---
  (swap! app-state* update :state/todos update-todo id title)
  (p/resolved (dissoc data :entity.todo/id :component/title)))
