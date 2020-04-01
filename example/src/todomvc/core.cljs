(ns todomvc.core
  (:require [datascript.core :as d]
            [mook.core :as m]
            [mook.react :as r]
            [orchestra-cljs.spec.test :as st]
            [react-dom :as react-dom]
            [todomvc.components :as c]))

(extend-type datascript.db/DB
  m/Watchable
  (m/listen! [this key f]
    (d/listen! this key (fn watch-changes [{:keys [db-after db-before] :as _transaction-data}]
                            (f {::new-value db-after
                                ::old-value db-before}))))
  (m/unlisten! [this key]
    (d/unlisten! this key)))

(defonce app-db* (d/create-conn {}))
(m/register-store! :todomvc.store/app-db* app-db*)

(defonce local-store* (atom {:local-store/active-filter :all
                             ;; :local/counter 0
                             }))
(m/register-store! :todomvc.store/local-store* local-store*)

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
    (m/wrap-with-mook-state-stores-context c/root)
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

