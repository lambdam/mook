# Mook React thin wrapper

Mook defines one function that makes it suitable to use with the included React
library:
[`create-element`](https://github.com/lambdam/mook/blob/f1f693b6b945a6a44fe9242eceb1c18b2de98be8/src/mook/react.cljs#L6)

It also defines various shortcuts in kebab-case instead of camel case (take a
look at the
[file](https://github.com/lambdam/mook/blob/master/src/mook/react.cljs)).

The mook version of the create-element is an optimized version of the one
described in this article:
[https://lambdam.com/blog/2020-09-react-wrapper/](https://lambdam.com/blog/2020-09-react-wrapper/)

This function is suitable for React (web) and React Native.

You can define your elements this way:

```cljs
;;React web

(def div
  (partial create-element "div"))

;; React Native

(def scroll-view
  (partial create-element (-> "react-native" js/require .-ScrollView)))

```

Since this can be cumbersome, mook exposes two utility macros.

One to define the elements from a map. The key is the symbol and the value is
the component:

```cljs
(ns my.app.tags
  (:require-macros [mook.react :refer [def-elems]]))

(def-elems {div    "div"
            a      "a"
            p      "p"
            img    "img"
            h5     "h5"
            h6     "h6"
            button "button"
            ul     "ul"
            li     "li"})
```

And another one, in the case of React web, that defines all offical HTML tags in
the current namespace:

```cljs
(ns my.app.tags
  (:require-macros [mook.react :refer [def-html-elems!]]))

(def-html-elems!)
```

⚠️ There is one important thing to note. This wrapper relies on
[cljs-bean](https://github.com/mfikes/cljs-bean). Thus, `create-element`receives
a regular clojure map. But the functional components receive a JS map that can
be easily used with the `cljs-bean.core/->clj` function.

Example:

```cljs
(ns my.app.components
  (:require [cljs-bean.core :as b]
            [mook.react :as mr]
            [my.app.tags :as t]))

(defn liste-entry [props']
  (let [{:keys [number]} (b/->clj props')]
    (t/li
      (str "Number " number))))

(defn root-list []
  (t/ul
    (for [number (range 10)]
      (mr/create-element list-entry {:key (str "react-key-" number)
                                     :number number}))))

(js/ReactDOM.render
  (create-element root-list)
  (js/document.getElementById "app-root"))
```
