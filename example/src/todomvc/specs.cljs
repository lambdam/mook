(ns todomvc.specs
  (:require [cljs.spec.alpha :as s]
            [promesa.core :as p]))

(s/def :component/title string?)

(s/def :action/promise
  p/promise?)

(s/def ::id string?)

(s/def :entity.todo/id ::id)
(s/def :entity.todo/title string?)
(s/def :entity.todo/completed? boolean?)

(s/def :entity/todo
  (s/keys :req [:entity.todo/id :entity.todo/title :entity.todo/completed?]))

(s/def :state/todos
  (s/coll-of :entity/todo :kind vector?))

(s/def :react-context/app-state
  (s/keys :req [:state/todos]))

(s/def :react-context/app-state*
  #(satisfies? cljs.core/IAtom %))
