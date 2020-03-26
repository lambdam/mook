(ns todomvc.helpers
  (:require [orchestra.core :refer-macros [defn-spec]]
            todomvc.specs ;; don't ns-clean
            ))

(defn-spec filter-todos :app-store/todos
  [todos :app-store/todos
   active-filter :local-store/active-filter]
  (case active-filter
    :all todos
    :active (filterv #(false? (:entity.todo/completed? %))
                     todos)
    :completed (filterv #(true? (:entity.todo/completed? %))
                        todos)))
