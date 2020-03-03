(ns todomvc.core
  (:require [cljs-bean.core :as b]
            [todomvc.components :as c]
            [todomvc.lib.react :as r]
            [todomvc.state :as state]))

(defn root-with-context []
  ((:provider state/app-state-context) {:value state/app-state*}
   (r/create-element c/root)))

(defn init! []
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
