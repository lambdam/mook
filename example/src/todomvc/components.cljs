(ns todomvc.components
  (:require [cljs-bean.core :as b]
            [clojure.string :as str]
            [mook.react :as r]
            [mook.hooks :as mkh]
            [promesa.core :as p]
            [todomvc.helpers :as h]
            [todomvc.commands :as c]
            [todomvc.elements :as el]
            [todomvc.lib.keys :as k]))

(defn todo-item [props]
  (let [state-stores (mkh/use-mook-state-stores)
        {:entity.todo/keys [id title completed?]} (b/->clj props)
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
                  :onChange (fn [_event]
                              (c/toggle-todo-status>> (assoc state-stores
                                                             :entity.todo/id
                                                             id)))})
        (el/label {:onDoubleClick (fn [e]
                                   (set-editing? true))}
          title)
        (el/button {:className "destroy"
                   :style {:cursor "pointer"}
                   :onClick (fn [_event]
                              (c/destroy-todo>> (assoc state-stores
                                                       :entity.todo/id
                                                       id)))}))
      (let [save-todo>> (fn save-todo>> [e]
                          (let [value (str/trim edit-text)]
                            (when-not (str/blank? value)
                              (p/chain
                                (c/update-todo>> (assoc state-stores
                                                        :component/title value
                                                        :entity.todo/id id))
                                #(set-editing? false)))))]
        (el/input {:ref edit-field-ref
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
  (let [counter (-> props b/->clj :todomvc-counter)
        state-stores (mkh/use-mook-state-stores)
        [title set-title] (r/use-state "")
        cntr (mkh/use-state-store :todomvc.store/local-store* (fn counter-handler [] counter))]
    (el/header {:className "header"}
      (el/h1 (str "todos" " " cntr))
      (el/input {:className "new-todo"
                :placeholder "What needs to be done?"
                :value title
                :onKeyDown #(case (-> % .-keyCode k/code->key)
                             :event/enter-key (let [value (str/trim title)]
                                                (.preventDefault %)
                                                (when-not (str/blank? value)
                                                  (c/create-new-todo>> (assoc state-stores :component/title value))
                                                  (set-title "")))
                             nil)
                :onChange #(-> % .-target .-value set-title)
                :autoFocus true}))))

(defn root [props]
  (let [state-stores (mkh/use-mook-state-stores)
        todos (mkh/use-state-store :todomvc.store/app-store* :app-store/todos)
        active-filter (mkh/use-state-store :todomvc.store/local-store* :local-store/active-filter)
        all-completed? (every? :entity.todo/completed? todos)]
    (r/fragment
      (el/section {:className "todoapp"}
        (r/create-element new-todo-form (b/->clj props))
        (when (not (empty? todos))
          (r/fragment
            (el/section {:className "main"}
              (el/input {:id"toggle-all"
                         :className "toggle-all"
                         :type "checkbox"
                         :checked all-completed?
                         :onChange (fn [_event]
                                     (c/toggle-all>> (assoc state-stores
                                                            :component/all-completed?
                                                            all-completed?)))})
              (el/label {:style {:cursor "pointer"}
                         :htmlFor "toggle-all"}
                #_"Mark all as complete")
              (el/ul {:className "todo-list"}
                (->> (h/filter-todos todos active-filter)
                     (mapv #(->> (merge % {:key (-> %
                                                    (select-keys [:entity.todo/id :entity.todo/completed?])
                                                    str)})
                                 (r/create-element todo-item)))
                     to-array)))
            (el/footer {:className "footer"}
              (el/span {:className "todo-count"}
                (let [len (count todos)]
                  (case len
                    0 "All completed"
                    1 "1 item left"
                    ;; else
                    (str len " items left"))))
              (el/ul {:className "filters"}
                (el/li {:style {:cursor "pointer"}}
                  (el/a {:onClick (fn [e]
                                    (.preventDefault e)
                                    (c/set-filter>> (assoc state-stores :local-store/active-filter :all)))
                         :className (when (= :all active-filter) "selected")}
                    "All"))
                (el/li {:style {:cursor "pointer"}}
                  (el/a {:onClick (fn [e]
                                    (.preventDefault e)
                                    (c/set-filter>> (assoc state-stores :local-store/active-filter :active)))
                         :className (when (= :active active-filter) "selected")}
                    "Active"))
                (el/li {:style {:cursor "pointer"}}
                  (el/a {:onClick (fn [e]
                                    (.preventDefault e)
                                    (c/set-filter>> (assoc state-stores :local-store/active-filter :completed)))
                         :className (when (= :completed active-filter) "selected")}
                    "Completed")))
              (when (some :entity.todo/completed? todos)
                (el/button {:className "clear-completed"
                           :onClick (fn [_event]
                                      (c/clear-completed-todos>> state-stores))}
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
