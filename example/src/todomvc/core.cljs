(ns todomvc.core
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            [cljs-bean.core :as b]))

(defn init! []
  (react-dom/render
    (react/createElement "h1" (b/->js {:style {:color "red"}})
                         "Hello from React")
    (js/document.getElementById "main-app")))

(defn main! []
  (init!))

(defn reload! []
  (init!))
