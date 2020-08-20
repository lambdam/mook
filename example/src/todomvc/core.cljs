(ns todomvc.core
  (:require [datascript.core :as d]
            [mook.core :as m]
            [mook.react :as r]
            [mook.react :as mr]
            [orchestra-cljs.spec.test :as st]
            react
            [react-dom :as react-dom]
            [todomvc.components :as c]))


;; (defonce counter* (atom 0))
;; (defonce initiated (atom false))

#_(defn root-comp [props]
  (r/create-element c/root (-> (cljs-bean.core/->clj props)
                               (assoc :todomvc-counter @counter*))))

(defn init! []
  #_(when-not @initiated
    (reset! initiated true))
  (let [out (-> (st/instrument)
                sort)]
    (js/console.log
      (str "Instrumented functions:\n" (with-out-str (cljs.pprint/pprint out)))))
  #_(swap! counter* inc)
  (react-dom/render
    (m/mook-state-store-container
      (mr/create-element c/root))
    (js/document.getElementById "main-app")))

(comment
  (init!)
  )

#_(defn reload! []
  (init!))

#_(when-not @initiated
  (js/setInterval
    #(init!)
    1000)
  (js/setInterval
    #(swap! local-store* update :local/counter inc)
    1500))

(init!)

