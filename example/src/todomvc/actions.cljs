(ns todomvc.actions
  (:require [cljs.spec.alpha :as s]
            [promesa.core :as p]
            [orchestra.core :refer-macros [defn-spec]]
            todomvc.specs ;; don't ns-clean
            ))

(defn-spec create-new-todo>> :action/promise
  [{:react-context/keys [app-state*]
    :component/keys [title]
    :as data}
   (s/keys :req [:react-context/app-state* :component/title])]
  ;; ---
  (swap! app-state* update :state/todos conj #:entity.todo{:id (str (random-uuid))
                                                           :title title
                                                           :completed? false})
  (p/resolved (dissoc data :component/keys)))

;; ---

(defn-spec toggle-todo-status>> :action/promise
  [{:react-context/keys [app-state*]
    :entity.todo/keys [id]
    :as data}
   (s/keys :req [:react-context/app-state* :entity.todo/id])]
  ;; ---
  (swap! app-state*
         (fn [app-state]
           (if-let [[index todo] (reduce-kv (fn [acc k v]
                                              (if (= id (:entity.todo/id v))
                                                (reduced [k v])
                                                acc))
                                            nil
                                            (:state/todos app-state))]
             (update-in app-state
                        [:state/todos index :entity.todo/completed?]
                        not)
             app-state)))
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
  (swap! app-state* update :state/todos #(destroy-todo % id))
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
