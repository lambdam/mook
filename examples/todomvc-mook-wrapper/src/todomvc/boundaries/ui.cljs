(ns todomvc.boundaries.ui
  (:require [clojure.spec.alpha :as s]))

(s/def ::all-completed? boolean?)

(s/def ::active-filter
  #{:all :active :completed})

(defn code->key [code]
  (case code
    27 ::escape-key
    13 ::enter-key
    nil))
