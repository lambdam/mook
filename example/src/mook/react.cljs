(ns mook.react
  (:require #_["react" :as react]
            #_["react-dom" :as react-dom]
            [react :as react]
            [cljs-bean.core :as b]
            [clojure.string :as str])
  (:require-macros [mook.react :refer [def-elems]]))

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

#_(defn render [react-el dom-el]
  (react-dom/render react-el dom-el))

(def html-tags
  ["h1" "h2" "h3" "h4" "h5" "h6"
   "div" "p" "a" "span" "section" "header" "footer"
   "input" "label" "button"
   "ul" "li"])

(def fragment
  (partial create-element react/Fragment))

(defn create-context [default-value]
  (let [context (react/createContext default-value)
        provider-class (.-Provider context)]
    {::context context
     ::provider-class provider-class
     ::provider (partial create-element provider-class)
     ::consumer (.-Consumer context)}))

(def use-context
  react/useContext)

(def use-ref
  react/useRef)

(def use-state
  react/useState)

(def use-effect
  react/useEffect)

(defn classes [prop-map]
  (->> (reduce (fn [acc [prop pred?]]
                 (if pred?
                   (conj acc (name prop))
                   acc))
               []
               prop-map)
       (str/join " ")))
