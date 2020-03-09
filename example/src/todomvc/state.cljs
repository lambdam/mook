(ns todomvc.state
  (:require [todomvc.lib.react :as r]))

(defonce local-store-subs-counter*
  (atom 0))

(defonce app-state*
  (atom {:state/todos []
         :state.local/active-filter :all}))

(def app-state-context
  (r/create-context app-state*))

(defn use-states []
  (let [app-state* (r/use-context (:context app-state-context))
        get-state* (fn get-state* [] {:react-context/app-state* app-state*})
        [value set-value!] (r/use-state (get-state*))]
    (r/use-effect (fn use-local-store-handler-effect []
                    (let [sub-id (swap! local-store-subs-counter* inc)]
                      (add-watch app-state*
                                 sub-id
                                 (fn add-local-store-watch [_sub-id _ref _old new]
                                   (set-value! (get-state*))))
                      (fn remove-local-store-watch []
                        (remove-watch app-state* sub-id))))
                  #js [])
    value))
