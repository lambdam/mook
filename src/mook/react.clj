(ns mook.react)

(defmacro def-elem [name]
  `(def ~(symbol name)
     (partial
       mook.react/create-element
       ~name)))

(defmacro def-elems [names]
  `(do ~@(for [name names]
           `(def ~(symbol name)
              (partial
                mook.react/create-element
                ~name)))))


(defmacro def-html-elems []
  (let [names ["h1" "h2" "h3" "h4" "h5" "h6"
               "div" "p" "a" "span" "section" "header" "footer"
               "input" "label" "button"
               "ul" "li"]]
    `(do ~@(for [name names]
             `(def ~(symbol name)
                (partial
                  mook.react/create-element
                  ~name))))))
