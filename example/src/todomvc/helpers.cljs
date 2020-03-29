(ns todomvc.helpers
  (:require [cljs.spec.alpha :as s]
            [orchestra.core :refer-macros [defn-spec]]
            todomvc.specs ;; don't ns-clean
            ))

(defn-spec filter-todos (s/coll-of :entity/todo)
  [todos (s/coll-of :entity/todo)
   active-filter :local-store/active-filter]
  (case active-filter
    :all todos
    :active (filterv #(false? (:todo/completed? %))
                     todos)
    :completed (filterv #(true? (:todo/completed? %))
                        todos)))
