(ns todomvc.specs
  (:require [cljs.spec.alpha :as s]
            [promesa.core :as p]))

(s/def :react-context/app-state*
  #(satisfies? cljs.core/IAtom %))

(s/def :component/title string?)

(s/def :action/promise
  p/promise?)
