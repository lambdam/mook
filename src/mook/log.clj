(ns mook.log)

(def color-log? false)

(defmacro dev-print! [data color]
  (when color-log?
    `(js/console.log (str "%c" (-> ~data cljs.pprint/pprint with-out-str))
                     (str "background: " ~color "; color: white"))))
