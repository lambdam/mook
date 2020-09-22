(ns todomvc.components
  (:require [cljs-bean.core :as b]
            [clojure.string :as str]
            [datascript.core :as d]
            [mook.core :as m]
            [mook.react :as r]
            [promesa.core :as p]
            [todomvc.boundaries.ui :as b-ui]
            [todomvc.commands :as cmd]
            [todomvc.elements :as el :include-macros true]
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
    (el/html
      [:li {:className (r/classes {:completed completed?
                                   :editing editing?})}
       [:div.view
        [:input.toggle {:type "checkbox"
                        :checked completed?
                        :style {:cursor "pointer"}
                        :onChange #(m/send-command>> {::m/type ::cmd/toggle-todo-status
                                                      :db/id id})}]
        [:label {:onDoubleClick (fn [e]
                                  (set-editing? true))}
         title]
        [:button.destroy {:style {:cursor "pointer"}
                          :onClick #(m/send-command>> {::m/type ::cmd/destroy-todo
                                                       :db/id id})}]
        (let [save-todo>> (fn save-todo>> [e]
                            (let [value (str/trim edit-text)]
                              (when-not (str/blank? value)
                                (p/chain
                                  (m/send-command>> {::m/type ::cmd/update-todo
                                                     :db/id id
                                                     :todo/title value})
                                  #(set-editing? false)))))]
          [:input.edit {:ref edit-field-ref
                        :value edit-text
                        :onBlur save-todo>>
                        :onChange #(-> % .-target .-value set-edit-text)
                        :onKeyDown #(case (-> % .-keyCode b-ui/code->key)
                                      ::b-ui/enter-key (save-todo>> %)
                                      ::b-ui/escape-key (do (set-editing? false)
                                                            (set-edit-text title))
                                      nil)}])]])))

(defn new-todo-form [props]
  (let [[title set-title] (r/use-state "")]
    (el/html
      [:header.header
       [:h1 "todos"]
       [:input.new-todo {:placeholder "What needs to be done?"
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
                         :autoFocus true}]])))

(defn root [props]
  (let [todos (m/use-param-state-store
                ::stores/app-db*
                {}
                (fn [db]
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
    (el/html
      [:*
       [:section.todoapp
        [:> new-todo-form]
        (when-not (empty? todos)
          [:*
           [:section.main
            [:input#toggle-all.toggle-all {:type "checkbox"
                                           :checked all-completed?
                                           :onChange #(m/send-command>> {::m/type ::cmd/toggle-all
                                                                         ::b-ui/all-completed? all-completed?})}]
            [:label {:style {:cursor "pointer"}
                     :htmlFor "toggle-all"}]
            [:ul.todo-list
             (for [todo (case active-filter
                          :all todos
                          :active (filterv #(false? (:todo/completed? %))
                                           todos)
                          :completed (filterv #(true? (:todo/completed? %))
                                              todos))]
               [:> todo-item {:key (->> [:db/id :todo/completed?]
                                        (map #(% todo))
                                        (str/join "-"))
                              :todo todo}])]]
           [:footer.footer
            [:span.todo-count (let [len (count todos)]
                                (case len
                                  0 "All completed"
                                  1 "1 item left"
                                  ;; else
                                  (str len " items left")))]
            (let [set-filter! (fn [type]
                                #(do (.preventDefault %)
                                     (m/send-command>> {::m/type ::cmd/set-filter
                                                        ::b-ui/active-filter type})))]
              [:ul.filters
               [:li {:style {:cursor "pointer"}}
                [:a {:onClick (set-filter! :all)
                     :className (when (= :all active-filter) "selected")}
                 "All"]]
               [:li {:style {:cursor "pointer"}}
                [:a {:onClick (set-filter! :active)
                     :className (when (= :active active-filter) "selected")}
                 "Active"]]
               [:li {:style {:cursor "pointer"}}
                [:a {:onClick (set-filter! :completed)
                     :className (when (= :completed active-filter) "selected")}
                 "Completed"]]
               ])
            (when (some :todo/completed? todos)
              [:button.clear-completed {:onClick #(m/send-command>> {::m/type ::cmd/clear-completed-todos})}
               "Clear completed"])]])]
       [:footer.info
        [:p "Double-click to edit a todo"]
        [:p "Written by" [:a {:href "https://github.com/lambdam"}
                          "Damien Ragoucy"]]
        [:p "Part of " [:a {:href "http://todomvc.com"}
                        "TodoMVC"]]]])))
