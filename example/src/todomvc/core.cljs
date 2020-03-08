(ns todomvc.core
  (:require [cljs-bean.core :as b]
            [orchestra-cljs.spec.test :as st]
            [todomvc.components :as c]
            [todomvc.lib.react :as r]
            [todomvc.state :as state]))

(defn root-with-context []
  ((:provider state/app-state-context) {:value state/app-state*}
   (r/create-element c/root)))

(defn init! []
  (let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  (r/render
    (r/create-element root-with-context)
    (js/document.getElementById "main-app")))

(comment
  (init!)
  )

(defn main! []
  (init!))

(defn reload! []
  (init!))
