(ns todomvc.commands
  (:require [cljs.spec.alpha :as s]
            [promesa.core :as p]
            [orchestra.core :refer-macros [defn-spec]]
            [mook.core :as c]
            todomvc.specs ;; don't ns-clean
            ))


(defn-spec ^:private create-new-todo :app-store/todos
  [todos :app-store/todos
   title string?]
  ;; ---
  (into [#:entity.todo{:id (str (random-uuid))
                       :title title
                       :completed? false}]
        todos))

(defn-spec create-new-todo>> :action/promise
  [{:todomvc.store/keys [app-store*]
    :component/keys [title]
    :as data}
   (s/keys :req [:todomvc.store/app-store* :component/title])]
  ;; ---
  (swap! app-store* update :app-store/todos create-new-todo title)
  (p/resolved (dissoc data :component/keys)))

;; ---

(defn-spec ^:private toggle-todo-status :todomvc.store/app-store
  [app-state :todomvc.store/app-store
   id :entity.todo/id]
  ;; ---
  (if-let [[index todo] (reduce-kv (fn [acc k v]
                                     (if (= id (:entity.todo/id v))
                                       (reduced [k v])
                                       acc))
                                   nil
                                   (:app-store/todos app-state))]
    (update-in app-state
               [:app-store/todos index :entity.todo/completed?]
               not)
    app-state))

(defn-spec toggle-todo-status>> :action/promise
  [{:todomvc.store/keys [app-store*]
    :entity.todo/keys [id]
    :as data}
   (s/keys :req [:todomvc.store/app-store* :entity.todo/id])]
  ;; ---
  (swap! app-store* #(toggle-todo-status % id))
  (p/resolved (dissoc data :entity.todo/id)))

;; ---

(defn-spec ^:private destroy-todo :app-store/todos
  [todos :app-store/todos
   id :entity.todo/id]
  (filterv #(not= (:entity.todo/id %) id)
           todos))

(defn-spec destroy-todo>> :action/promise
  [{:todomvc.store/keys [app-store*]
    :entity.todo/keys [id]
    :as data}
   (s/keys :req [:todomvc.store/app-store* :entity.todo/id])]
  ;; ---
  (swap! app-store* update :app-store/todos destroy-todo id)
  (p/resolved (dissoc data :entity.todo/id)))

;; ---

;; TODO: does (fn-spec ...) exists so that it is not
;; mandatory to split the async function?
(defn-spec ^:private clear-completed-todos :app-store/todos
  [todos :app-store/todos]
  (filterv #(false? (:entity.todo/completed? %)) todos))

(defn-spec clear-completed-todos>> :action/promise
  [{:todomvc.store/keys [app-store*]
    :as data}
   (s/keys :req [:todomvc.store/app-store*])]
  ;; ---
  (swap! app-store* update :app-store/todos clear-completed-todos)
  (p/resolved data))

;; ---

(defn-spec ^:private toggle-all :app-store/todos
  [todos :app-store/todos
   all-completed? :component/all-completed?]
  ;; ---
  (mapv #(assoc % :entity.todo/completed? (not all-completed?))
        todos))

(defn-spec toggle-all>> :action/promise
  [{:todomvc.store/keys [app-store*]
    :component/keys [all-completed?]
    :as data}
   (s/keys :req [:todomvc.store/app-store* :component/all-completed?])]
  ;; ---
  (swap! app-store* update :app-store/todos toggle-all all-completed?)
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

(defn-spec ^:private update-todo :app-store/todos
  [todos :app-store/todos
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
  [{:todomvc.store/keys [app-store*]
    :entity.todo/keys [id]
    :component/keys [title]
    :as data}
   (s/keys :req [:todomvc.store/app-store* :component/title :entity.todo/id])]
  ;; ---
  (swap! app-store* update :app-store/todos update-todo id title)
  (p/resolved (dissoc data :entity.todo/id :component/title)))
