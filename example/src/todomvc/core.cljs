(ns todomvc.core
  (:require ["react" :as react]
            ["react-dom" :as react-dom]))

(defn init! []
  (react-dom/render
    (react/createElement "h1" nil "Hello from React")
    (js/document.getElementById "main-app")))

(defn main! []
  (init!))

(defn reload! []
  (init!))
