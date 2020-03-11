(ns todomvc.components
  (:require [cljs-bean.core :as b]
            [clojure.string :as str]
            [promesa.core :as p]
            [todomvc.actions :as a]
            [todomvc.helpers :as h]
            [todomvc.lib.keys :as k]
            [todomvc.lib.react :as r]
            [todomvc.state :as state]))

(defn todo-item [props]
  (let [states (state/use-states)
        {:entity.todo/keys [id title completed?]} (b/->clj props)
        [editing? set-editing?] (r/use-state false)
        [edit-text set-edit-text] (r/use-state title)
        edit-field-ref (r/use-ref nil)]
    (r/use-effect (fn focus-field []
                    (when editing?
                      (-> edit-field-ref .-current .focus))
                    (fn clean []))
                  #js [editing?])
    (r/li {:className (r/classes {:completed completed?
                                  :editing editing?})}
      (r/div {:className "view"}
        (r/input {:className "toggle"
                  :type "checkbox"
                  :checked completed?
                  :style {:cursor "pointer"}
                  :onChange (fn [_event]
                              (a/toggle-todo-status>> (assoc states
                                                             :entity.todo/id
                                                             id)))})
        (r/label {:onDoubleClick (fn [e]
                                   (set-editing? true))}
          title)
        (r/button {:className "destroy"
                   :style {:cursor "pointer"}
                   :onClick (fn [_event]
                              (a/destroy-todo>> (assoc states
                                                       :entity.todo/id
                                                       id)))}))
      (let [save-todo>> (fn save-todo>> [e]
                          (let [value (str/trim edit-text)]
                            (when-not (str/blank? value)
                              (p/chain
                                (a/update-todo>> (assoc states
                                                        :component/title value
                                                        :entity.todo/id id))
                                #(set-editing? false)))))]
        (r/input {:ref edit-field-ref
                  :className "edit"
                  :value edit-text
                  :onBlur save-todo>>
                  :onChange #(-> % .-target .-value set-edit-text)
                  :onKeyDown #(case (-> % .-keyCode k/code->key)
                                :event/enter-key (save-todo>> %)
                                :event/escape-key (do (set-editing? false)
                                                      (set-edit-text title))
                                nil)})))))

(defn new-todo-form [props]
  (let [states (state/use-states)
        [title set-title] (r/use-state "")]
    (r/header {:className "header"}
      (r/h1 "todos")
      (r/input {:className "new-todo"
                :placeholder "What needs to be done?"
                :value title
                :onKeyDown #(case (-> % .-keyCode k/code->key)
                             :event/enter-key (let [value (str/trim title)]
                                                (.preventDefault %)
                                                (when-not (str/blank? value)
                                                  (a/create-new-todo>> (assoc states :component/title value))
                                                  (set-title "")))
                             nil)
                :onChange #(-> % .-target .-value set-title)
                :autoFocus true}))))

(defn root [props]
  (let [{:react-context/keys [app-state*] :as states} (state/use-states)
        todos (:state/todos @app-state*)
        all-completed? (every? :entity.todo/completed? todos)]
    (r/fragment
      (r/section {:className "todoapp"}
        (r/create-element new-todo-form)
        (when (not (empty? todos))
          (r/fragment
            (r/section {:className "main"}
              (r/input {:id"toggle-all"
                        :className "toggle-all"
                        :type "checkbox"
                        :checked all-completed?
                        :onChange (fn [_event]
                                    (a/toggle-all>> (assoc states
                                                           :component/all-completed?
                                                           all-completed?)))})
              (r/label {:style {:cursor "pointer"}
                        :htmlFor "toggle-all"}
                #_"Mark all as complete")
              (r/ul {:className "todo-list"}
                    (->> (h/filter-todos todos (:state.local/active-filter @app-state*))
                         (mapv #(->> (merge % {:key (-> %
                                                        (select-keys [:entity.todo/id :entity.todo/completed?])
                                                        str)})
                                     (r/create-element todo-item)))
                         to-array)))
            (r/footer {:className "footer"}
              (r/span {:className "todo-count"}
                (let [len (count todos)]
                  (case len
                    0 "All completed"
                    1 "1 item left"
                    ;; else
                    (str len " items left"))))
              (let [{:state.local/keys [active-filter]} @app-state*]
                (r/ul {:className "filters"}
                  (r/li
                    (r/a {:onClick (fn [e]
                                     (.preventDefault e)
                                     (a/set-filter>> (assoc states :state.local/active-filter :all)))
                          :className (when (= :all active-filter) "selected")}
                      "All"))
                  (r/li
                    (r/a {:onClick (fn [e]
                                     (.preventDefault e)
                                     (a/set-filter>> (assoc states :state.local/active-filter :active)))
                          :className (when (= :active active-filter) "selected")}
                      "Active"))
                  (r/li
                    (r/a {:onClick (fn [e]
                                     (.preventDefault e)
                                     (a/set-filter>> (assoc states :state.local/active-filter :completed)))
                          :className (when (= :completed active-filter) "selected")}
                      "Completed"))))
              (when (some :entity.todo/completed? todos)
                (r/button {:className "clear-completed"
                           :onClick (fn [_event]
                                      (a/clear-completed-todos>> states))}
                  "Clear completed!"))))))
      (r/footer {:className "info"}
        (r/p "Double-click to edit a todo")
        (r/p
          "Written by "
          (r/a {:href "https://github.com/lambdam"}
            "Damien Ragoucy"))
        (r/p
          "Part of "
          (r/a {:href "http://todomvc.com"}
            "TodoMVC"))))))
