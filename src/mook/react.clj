(ns mook.react
  (:require [clojure.spec.alpha :as s]))

#_(defmacro def-elem [name]
  `(def ~(symbol name)
     (partial
       mook.react/create-element
       ~name)))

(def html-tags
  ["h1" "h2" "h3" "h4" "h5" "h6"
   "div" "p" "a" "span" "section" "header" "footer"
   "input" "label" "button"
   "ul" "li"])

(defmacro def-elems [names]
  (s/assert (s/map-of symbol? any?) names)
  `(do ~@(for [[sym target] names]
           `(def ~sym
              (partial
                mook.react/create-element
                ~target)))))
