(ns todomvc.components
  (:require [cljs-bean.core :as b]
            [clojure.string :as str]
            [todomvc.actions :as a]
            [todomvc.lib.keys :as k]
            [todomvc.lib.react :as r]
            [todomvc.state :as state]))

(defn todo-item [props]
  (let [states (state/use-states)
        {:entity.todo/keys [id title completed?]} (b/->clj props)
        edit-field-ref (r/use-ref nil)]
    (r/li {:className (r/classes {:completed false
                                  :editing false})}
      (r/div {:className "view"}
        (r/input {:className "toggle"
                  :type "checkbox"
                  :checked completed?
                  :onChange (fn [_event]
                              (a/toggle-todo-status>> (assoc states
                                                             :entity.todo/id
                                                             id)))})
        (r/label {:onDoubleClick (fn [])}
          title)
        (r/button {:className "destroy"
                   :onClick (fn [_event]
                              (a/destroy-todo>> (assoc states
                                                       :entity.todo/id
                                                       id)))}))
      (r/input {:ref edit-field-ref
                :className "edit"
                ;; :value nil
                :onBlur (fn [])
                :onChange (fn [])
                :onKeyDown (fn [e]
                             (js/console.log e))}))))

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
  (let [states (state/use-states)
        {:react-context/keys [app-state*]} (state/use-states)
        todos (:state/todos @app-state*)
        some-completed? (boolean (some :entity.todo/completed? todos))]
    (r/fragment
      (r/section {:className "todoapp"}
        (r/create-element new-todo-form)
        (when (not (empty? todos))
          (r/fragment
            (r/section {:className "main"}
              (r/input {:id"toggle-all"
                        :className "toggle-all"
                        :type "checkbox"
                        :checked (not some-completed?)
                        :onChange (fn [_event]
                                    (a/toggle-all>> (assoc states
                                                           :component/some-completed?
                                                           some-completed?)))})
              (r/label {:htmlFor "toggle-all"}
                #_"Mark all as complete")
              (r/ul {:className "todo-list"}
                    (->> todos
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
              (r/ul {:className "filters"}
                (r/li
                  (r/a {:href "#/" :className "selected"}
                    "All"))
                (r/li
                  (r/a {:href "#/active"}
                    "Active"))
                (r/li
                  (r/a {:href "#/completed"}
                       "Completed")))
              (when some-completed?
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
