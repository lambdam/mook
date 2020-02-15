(ns todomvc.state
  (:require [todomvc.lib.react :as r]))

(defonce app-state*
  (atom {:todos [{:id (random-uuid) :title "Do foo" :completed? false}
                 {:id (random-uuid) :title "Do bar" :completed? true}
                 {:id (random-uuid) :title "Do baz" :completed? false}]}))

(def app-state-context
  (r/create-context app-state*))

(defn use-states []
  {:app-state* (r/use-context (:context app-state-context))})
