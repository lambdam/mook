(ns mook.react
  (:require [clojure.string :as str]
            [cljs-bean.core :as b]))

(def ^:private createElement
  js/React.createElement)

(defn create-element
  ([comp]
   (createElement comp nil))
  ([comp opts]
   (if (map? opts)
     (createElement comp (b/->js opts))
     (createElement comp nil opts)))
  ([comp opts el1]
   (if (map? opts)
     (createElement comp (b/->js opts) el1)
     (createElement comp nil opts el1)))
  ([comp opts el1 el2]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2)
     (createElement comp nil opts el1 el2)))
  ([comp opts el1 el2 el3]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3)
     (createElement comp nil opts el1 el2 el3)))
  ([comp opts el1 el2 el3 el4]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3 el4)
     (createElement comp nil opts el1 el2 el3 el4)))
  ([comp opts el1 el2 el3 el4 el5]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3 el4 el5)
     (createElement comp nil opts el1 el2 el3 el4 el5)))
  ([comp opts el1 el2 el3 el4 el5 el6]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6)
     (createElement comp nil opts el1 el2 el3 el4 el5 el6)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7)
     (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8)
     (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9)
     (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10]
   (if (map? opts)
     (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9 el10)
     (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10)))
  ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 & children]
   (if (map? opts)
     (apply createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 children)
     (apply createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 children))))

(def fragment
  (partial create-element js/React.Fragment))

(defn create-context [default-value]
  (let [context (js/React.createContext default-value)
        provider-class (.-Provider context)]
    {::context context
     ::provider-class provider-class
     ::provider (partial create-element provider-class)
     ::consumer (.-Consumer context)}))

(def use-state
  js/React.useState)

(def use-effect
  js/React.useEffect)

(def use-context
  js/React.useContext)

(def use-ref
  js/React.useRef)

;; Utility

(defn classes [prop-map]
  (->> prop-map
       (reduce-kv (fn [acc prop pred?]
                    (if pred?
                      (conj acc (name prop))
                      acc))
                  [])
       (str/join " ")))
