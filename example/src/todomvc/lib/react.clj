(ns todomvc.lib.react)

(defmacro def-elem [name]
  `(def ~(symbol name)
     (partial
       todomvc.lib.react/create-element
       ~name)))

(defmacro def-elems [names]
  `(do ~@(for [name names]
           `(def ~(symbol name)
              (partial
                todomvc.lib.react/create-element
                ~name)))))
