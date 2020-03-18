(ns todomvc.core
  (:require [mook.core :as mk]
            [mook.react :as r]
            [mook.hooks :as h]
            [orchestra-cljs.spec.test :as st]
            [react-dom :as react-dom]
            [todomvc.components :as c]))

(mk/register-store! :local-store (atom {}))

#_(defn root-with-context []
    ((:provider states/app-state-context) {:value states/app-state*}
     (r/create-element c/root)))

(defn root-with-context []
  ((::r/provider h/state-stores-context) {:value h/stores*}
   (r/create-element c/root)))

(defn init! []
  (let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  (react-dom/render
    (r/create-element root-with-context)
    (js/document.getElementById "main-app")))

(comment
  (init!)
  )

#_(defn reload! []
  (init!))

(init!)
