(ns mook.react
  (:require [clojure.spec.alpha :as s]))

#_(defmacro def-elem [name]
  `(def ~(symbol name)
     (partial
       mook.react/create-element
       ~name)))

(defmacro def-elems [names]
  (s/assert (s/map-of symbol? any?) names)
  `(do ~@(for [[sym target] names]
           `(def ~sym
              (partial
                mook.react/create-element
                ~target)))))
