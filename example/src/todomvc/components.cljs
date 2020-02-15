(ns todomvc.components
  (:require [cljs-bean.core :as b]
            [todomvc.lib.react :as r]
            [todomvc.state :as state]))

(def ^:const escape-key 27)
(def ^:const enter-key 13)

(defn todo-item [props]
  (let [{:keys [title]} (b/->clj props)
        edit-field-ref (r/use-ref nil)]
    (r/li {:className (r/classes {:completed false
                                  :editing false})}
      (r/div {:className "view"}
        (r/input {:className "toggle"
                  :type "checkbox"
                  :checked true
                  :onChange (fn [])})
        (r/label {:onDoubleClick (fn [])}
                 title)
        (r/button {:className "destroy"
                   :onClick (fn [])}))
      (r/input {:ref edit-field-ref
                :className "edit"
                ;; :value nil
                :onBlur (fn [])
                :onChange (fn [])
                :onKeyDown (fn [])}))))

(defn root [props]
  (let [{:keys [app-state*]} (state/use-states)
        todos (:todos @state/app-state*)]
    (r/fragment
      (r/section {:className "todoapp"}
        (r/header {:className "header"}
          (r/h1 "todos")
          (r/input {:className "new-todo" :placeholder "What needs to be done?" :autoFocus true}))
        (when (not (empty? todos))
          (r/fragment
            (r/section {:className "main"}
              (r/input {:id "toggle-all" :className "toggle-all" :type "checkbox"})
              (r/label {:htmlFor "toggle-all"}
                "Mark all as complete")
              (r/ul {:className "todo-list"}
                (to-array (mapv #(->> (merge % {:key (-> %
                                                         (select-keys [:id :completed?])
                                                         str)})
                                      (r/create-element todo-item))
                                todos))))
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
                    "Completed")))))))
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
