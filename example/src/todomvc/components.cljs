(ns todomvc.components
  (:require [cljs-bean.core :as b]
            [clojure.string :as str]
            [datascript.core :as d]
            [mook.core :as m]
            [mook.react :as r]
            [promesa.core :as p]
            [todomvc.boundaries.ui :as b-ui]
            [todomvc.commands :as cmd]
            [todomvc.elements :as el]
            [todomvc.stores :as stores]))

(defn todo-item [props]
  (let [{:db/keys [id] :todo/keys [title completed?]} (-> props b/->clj :todo)
        [editing? set-editing?] (r/use-state false)
        [edit-text set-edit-text] (r/use-state title)
        edit-field-ref (r/use-ref nil)]
    (r/use-effect (fn focus-field []
                    (when editing?
                      (-> edit-field-ref .-current .focus))
                    (fn clean []))
                  #js [editing?])
    (el/li {:className (r/classes {:completed completed?
                                   :editing editing?})}
      (el/div {:className "view"}
        (el/input {:className "toggle"
                   :type "checkbox"
                   :checked completed?
                   :style {:cursor "pointer"}
                   :onChange #(m/send-command>> {::m/type ::cmd/toggle-todo-status
                                                 :db/id id})})
        (el/label {:onDoubleClick (fn [e]
                                   (set-editing? true))}
          title)
        (el/button {:className "destroy"
                    :style {:cursor "pointer"}
                    :onClick #(m/send-command>> {::m/type ::cmd/destroy-todo
                                                 :db/id id})}))
      (let [save-todo>> (fn save-todo>> [e]
                          (let [value (str/trim edit-text)]
                            (when-not (str/blank? value)
                              (p/chain
                                (m/send-command>> {::m/type ::cmd/update-todo
                                                   :db/id id
                                                   :todo/title value})
                                #(set-editing? false)))))]
        (el/input {:ref edit-field-ref
                   :className "edit"
                   :value edit-text
                   :onBlur save-todo>>
                   :onChange #(-> % .-target .-value set-edit-text)
                   :onKeyDown #(case (-> % .-keyCode b-ui/code->key)
                                 ::b-ui/enter-key (save-todo>> %)
                                 ::b-ui/escape-key (do (set-editing? false)
                                                       (set-edit-text title))
                                 nil)})))))

(defn new-todo-form [props]
  (let [[title set-title] (r/use-state "")]
    (el/header {:className "header"}
      (el/h1 "todos")
      (el/input {:className "new-todo"
                 :placeholder "What needs to be done?"
                 :value title
                 :onKeyDown #(case (-> % .-keyCode b-ui/code->key)
                               ::b-ui/enter-key (let [value (str/trim title)]
                                                  (.preventDefault %)
                                                  (when-not (str/blank? value)
                                                    (m/send-command>> {::m/type ::cmd/create-new-todo
                                                                       :todo/title value})
                                                    (set-title "")))
                               nil)
                 :onChange #(-> % .-target .-value set-title)
                 :autoFocus true}))))

(defn root [props]
  (let [todos (m/use-param-state-store
                ::stores/app-db*
                {}
                (fn [db _]
                  (as-> db <>
                    (d/q '[:find [?e ...]
                           :where [?e :todo/title]]
                         <>)
                    (d/pull-many db '[*] <>)
                    (sort-by :todo/created-at
                             #(compare %2 %1)
                             <>))))
        active-filter (m/use-state-store ::stores/local-store* ::b-ui/active-filter)
        all-completed? (every? :todo/completed? todos)]
    (r/fragment
      (el/section {:className "todoapp"}
        (r/create-element new-todo-form)
        (when (not (empty? todos))
          (r/fragment
            (el/section {:className "main"}
              (el/input {:id"toggle-all"
                         :className "toggle-all"
                         :type "checkbox"
                         :checked all-completed?
                         :onChange #(m/send-command>> {::m/type ::cmd/toggle-all
                                                       ::b-ui/all-completed? all-completed?})})
              (el/label {:style {:cursor "pointer"}
                         :htmlFor "toggle-all"}
                #_"Mark all as complete")
              (el/ul {:className "todo-list"}
                (->> (case active-filter
                       :all todos
                       :active (filterv #(false? (:todo/completed? %))
                                        todos)
                       :completed (filterv #(true? (:todo/completed? %))
                                           todos))
                     (mapv (fn [todo]
                             (r/create-element todo-item {:key (->> [:db/id :todo/completed?]
                                                                    (map #(% todo))
                                                                    (str/join "-"))
                                                          :todo todo})))
                     to-array)))
            (el/footer {:className "footer"}
              (el/span {:className "todo-count"}
                (let [len (count todos)]
                  (case len
                    0 "All completed"
                    1 "1 item left"
                    ;; else
                    (str len " items left"))))
              (let [set-filter! (fn [type]
                                  #(do (.preventDefault %)
                                       (m/send-command>> {::m/type ::cmd/set-filter
                                                          ::b-ui/active-filter type})))]
                (el/ul {:className "filters"}
                  (el/li {:style {:cursor "pointer"}}
                    (el/a {:onClick (set-filter! :all)
                           :className (when (= :all active-filter) "selected")}
                      "All"))
                  (el/li {:style {:cursor "pointer"}}
                    (el/a {:onClick (set-filter! :active)
                           :className (when (= :active active-filter) "selected")}
                      "Active"))
                  (el/li {:style {:cursor "pointer"}}
                    (el/a {:onClick (set-filter! :completed)
                           :className (when (= :completed active-filter) "selected")}
                      "Completed"))))
              (when (some :todo/completed? todos)
                (el/button {:className "clear-completed"
                            :onClick #(m/send-command>> {::m/type ::cmd/clear-completed-todos})}
                  "Clear completed!"))))))
      (el/footer {:className "info"}
        (el/p "Double-click to edit a todo")
        (el/p
          "Written by "
          (el/a {:href "https://github.com/lambdam"}
            "Damien Ragoucy"))
        (el/p
          "Part of "
          (el/a {:href "http://todomvc.com"}
            "TodoMVC"))))))
