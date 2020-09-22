(ns todomvc.elements
  (:require [hicada.compiler :as c]))

;; Taken from master. 0.1.8 doesn't have multi arity :>
(def default-handlers {:> (fn
                            ([_ klass]
                             [klass {} nil])
                            ([_ klass attrs & children]
                             (if (map? attrs)
                               [klass attrs children]
                               [klass {} (cons attrs children)])))
                       :* (fn [_ attrs & children]
                            (if (map? attrs)
                              ['js/React.Fragment attrs children]
                              ['js/React.Fragment {} (cons attrs children)]))})

(defmacro html [body]
  (c/compile body
             {:create-element 'js/React.createElement
              :transform-fn (comp)
              :array-children? false}
             default-handlers
             &env))
