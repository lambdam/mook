(ns todomvc.core
  (:require [datascript.core :as d]
            [mook.core :as m]
            [mook.react :as r]
            [mook.react :as mr]
            [orchestra-cljs.spec.test :as st]
            [promesa.core :as p]
            react
            [react-dom :as react-dom]
            [todomvc.components :as c]
            [todomvc.stores :as stores]))

(def wrap-ref-state-stores
  (m/create-state-store-wrapper!
    [{::m/store-key ::stores/local-store*
      ::m/state-key ::stores/local-store
      ::m/store*    stores/local-store*}
     {::m/store-key ::stores/app-db*
      ::m/state-key ::stores/app-db
      ::m/store*    stores/app-db*}]))

(defn wrap-console-log [command]
  (fn process-console-log>> [data]
    (println "Data before\n" data)
    (-> (command data)
        (p/then (fn [data']
                  (println "Data after\n" data')
                  data')))))

(defn init! []
  (let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  (m/init-mook! {::m/command-middlewares [wrap-ref-state-stores
                                          wrap-console-log
                                          ;; Add as many middlewares as you wish.
                                          ;; They will be applied in the declared order.
                                          ]})
  (react-dom/render
    (mr/create-element c/root)
    (js/document.getElementById "main-app")))

(init!)
