(ns todomvc.core
  (:require-macros [todomvc.elements :as el])
  (:require [datascript.core :as d]
            [mook.core :as m]
            [mook.react :as r]
            [mook.react :as mr]
            [orchestra-cljs.spec.test :as st]
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

(defn init! []
  (let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  (m/init-mook! {::m/command-middlewares [wrap-ref-state-stores]})
  (react-dom/render
    (el/html [:> c/root])
    (js/document.getElementById "main-app")))

(init!)

