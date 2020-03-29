(ns todomvc.core
  (:require [mook.core :as mk]
            [mook.react :as r]
            [mook.hooks :as h]
            [orchestra-cljs.spec.test :as st]
            [react-dom :as react-dom]
            [todomvc.components :as c]))

(defonce app-store* (atom {:app-store/todos []}))
(mk/register-store! :todomvc.store/app-store* app-store*)

(defonce local-store* (atom {:local-store/active-filter :all
                             :local/counter 0}))
(mk/register-store! :todomvc.store/local-store* local-store*)

(defonce counter* (atom 0))
(defonce initiated (atom false))

(defn root-comp [props]
  (r/create-element c/root (-> (cljs-bean.core/->clj props)
                               (assoc :todomvc-counter @counter*))))

(defn init! []
  (when-not @initiated
    (reset! initiated true))
  #_(let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  (println (swap! counter* inc))
  (react-dom/render
    (h/wrap-with-mook-state-stores-context root-comp)
    (js/document.getElementById "main-app")))

(comment
  (init!)
  )

#_(defn reload! []
  (init!))

(when-not @initiated
  (js/setInterval
    #(init!)
    1000)
  (js/setInterval
    #(swap! local-store* update :local/counter inc)
    1500))

(init!)

