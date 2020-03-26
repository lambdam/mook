(ns todomvc.specs
  (:require [cljs.spec.alpha :as s]
            [promesa.core :as p]))

;; # Commands

(s/def :component/title string?)
(s/def :component/all-completed? boolean?)

(s/def :action/promise
  p/promise?)

(s/def ::id string?)

;; # Stores

;; ## Local store

(s/def :local-store/active-filter
  #{:all :active :completed})

(s/def :todomvc.store/local-store
  (s/keys :req [:local-store/active-filter]))

(s/def :todomvc.store/local-store*
  #(satisfies? cljs.core/IAtom %))

;; ## App store

(s/def :entity.todo/id ::id)
(s/def :entity.todo/title string?)
(s/def :entity.todo/completed? boolean?)

(s/def :entity/todo
  (s/keys :req [:entity.todo/id :entity.todo/title :entity.todo/completed?]))

(s/def :app-store/todos
  (s/coll-of :entity/todo :kind vector?))

(s/def :todomvc.store/app-store
  (s/keys :req [:app-store/todos]))

(s/def :todomvc.store/app-store*
  #(satisfies? cljs.core/IAtom %))
