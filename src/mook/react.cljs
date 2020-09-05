(ns mook.react
  (:require [cljs-bean.core :as b]
            [react :as react]
            [clojure.string :as str]))

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
  ([comp opts el1 el2 el3 el4 el5]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3 el4 el5)
     (react/createElement comp nil opts el1 el2 el3 el4 el5)))
  ([comp opts el1 el2 el3 el4 el5 el6]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6)
     (react/createElement comp nil opts el1 el2 el3 el4 el5 el6)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7)
     (react/createElement comp nil opts el1 el2 el3 el4 el5 el6 el7)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8)
     (react/createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9)
     (react/createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10]
   (if (map? opts)
     (react/createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9 el10)
     (react/createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 & children]
   (if (map? opts)
     (apply react/createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 children)
     (apply react/createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 children))))

(def fragment
  (partial create-element react/Fragment))

(defn create-context [default-value]
  (let [context (react/createContext default-value)
        provider-class (.-Provider context)]
    {::context context
     ::provider-class provider-class
     ::provider (partial create-element provider-class)
     ::consumer (.-Consumer context)}))

(def use-state
  react/useState)

(def use-effect
  react/useEffect)

(def use-context
  react/useContext)

(def use-ref
  react/useRef)

;; Utility

(defn classes [prop-map]
  (->> prop-map
       (reduce-kv (fn [acc prop pred?]
                    (if pred?
                      (conj acc (name prop))
                      acc))
                  [])
       (str/join " ")))
