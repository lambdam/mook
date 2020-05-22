(ns mook.react
  (:require #_["react" :as react]
            #_["react-dom" :as react-dom]
            [react :as react]
            [cljs-bean.core :as b]
            [clojure.string :as str])
  (:require-macros [mook.react :refer [def-elems]]))

(def ^:private createElement
  react/createElement)

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
  (->> (reduce (fn [acc [prop pred?]]
                 (if pred?
                   (conj acc (name prop))
                   acc))
               []
               prop-map)
       (str/join " ")))
