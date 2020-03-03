(ns todomvc.lib.react
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            [cljs-bean.core :as b]
            [clojure.string :as str])
  (:require-macros [todomvc.lib.react :refer [def-elems]]))

(defn create-element
  ([comp]
   (react/createElement comp nil))
  ([comp opts]
   (if (map? opts)
     (react/createElement comp (b/->js opts))
     (react/createElement comp nil opts)))
  ([comp opts el1]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1)
     (react/createElement comp nil opts el1)))
  ([comp opts el1 el2]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2)
     (react/createElement comp nil opts el1 el2)))
  ([comp opts el1 el2 el3]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3)
     (react/createElement comp nil opts el1 el2 el3)))
  ([comp opts el1 el2 el3 el4]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3 el4)
     (react/createElement comp nil opts el1 el2 el3 el4)))
  ([comp opts el1 el2 el3 el4 & children]
   (if (map? opts)
     (apply react/createElement comp (b/->js opts) el1 el2 el3 el4 children)
     (apply react/createElement comp nil opts el1 el2 el3 el4 children))))

(defn render [react-el dom-el]
  (react-dom/render react-el dom-el))

(def-elems
  ["h1" "h2" "h3" "h4" "h5" "h6"
   "div" "p" "a" "span" "section" "header" "footer"
   "input" "label" "button"
   "ul" "li"])

(def fragment
  (partial create-element (.-Fragment react)))

(def ^:private createContext
  (.-createContext react))

(defn create-context [default-value]
  (let [context (createContext default-value)
        provider-class (.-Provider context)
        provider (partial create-element provider-class)
        consumer (.-Consumer context)]
    {:context context
     :provider-class provider-class
     :provider provider
     :consumer consumer}))

(def use-context
  (.-useContext react))

(def use-ref
  (.-useRef react))

(def use-state
  (.-useState react))

(def use-effect
  (.-useEffect react))

(defn classes [prop-map]
  (->> (reduce (fn [acc [prop pred?]]
                 (if pred?
                   (conj acc (name prop))
                   acc))
               []
               prop-map)
       (str/join " ")))
