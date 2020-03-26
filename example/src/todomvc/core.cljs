(ns todomvc.core
  (:require [mook.core :as mk]
            [mook.react :as r]
            [mook.hooks :as h]
            [orchestra-cljs.spec.test :as st]
            [react-dom :as react-dom]
            [todomvc.components :as c]))

(mk/register-store! :todomvc.store/app-store* (atom {:app-store/todos []}))

(mk/register-store! :todomvc.store/local-store* (atom {:local-store/active-filter :all}))

(defn init! []
  (let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  (react-dom/render
    (h/wrap-with-mook-state-stores-context c/root)
    (js/document.getElementById "main-app")))

(comment
  (init!)
  )

#_(defn reload! []
  (init!))

(init!)
