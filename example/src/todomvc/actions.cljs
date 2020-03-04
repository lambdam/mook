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
  (swap! app-state* update :state/todos conj #:todo{:id (random-uuid)
                                                    :title title
                                                    :completed? false})
  (p/resolved (dissoc data :component/keys)))
