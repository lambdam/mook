(ns todomvc.core
  (:require [datascript.core :as d]
            [mook.core :as m]
            [mook.react :as r]
            [mook.react :as mr]
            [orchestra-cljs.spec.test :as st]
            react
            [react-dom :as react-dom]
            [todomvc.components :as c]
            [todomvc.elements :as el]))


(defn init! []
  (let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  (react-dom/render
    (m/mook-state-store-container
      (el/html
        [:> c/root]))
    (js/document.getElementById "main-app")))

(init!)

