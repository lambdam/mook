(ns mook.command
  (:require [cljs.spec.alpha :as s]
            [promesa.core :as p]))

(defn command-dispatch [[type _data :as _event] ]
  type)

(s/fdef command-dispatch
  :args (s/cat :event (s/and vector?
                             (s/cat :event-type keyword? :data map?)))
  :ret keyword?)

(defmulti command>> command-dispatch)

(s/fdef command>>
  :args (s/cat :data map?)
  :ret p/promise?)

(defmethod command>> :default [[type _ :as event]]
  (p/rejected (ex-info (str "No dispatch method for type: " type)
                       event)))
