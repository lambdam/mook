(ns todomvc.lib.keys)

(defn code->key [code]
  (case code
    27 :event/escape-key
    13 :event/enter-key
    nil))
