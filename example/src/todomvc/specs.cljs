(ns todomvc.specs
  (:require [cljs.spec.alpha :as s]
            [datascript.core :as d]
            [promesa.core :as p]))

;; # Commands

(s/def :component/all-completed? boolean?)

(s/def :action/promise
  p/promise?)

;; # Stores

;; ## Local store

(s/def :local-store/active-filter
  #{:all :active :completed})

(s/def :todomvc.store/local-store
  (s/keys :req [:local-store/active-filter]))

(s/def :todomvc.store/local-store*
  #(satisfies? cljs.core/IAtom %))

;; ## App db

(s/def :db/id integer?)

(s/def :todo/title string?)
(s/def :todo/completed? boolean?)
(s/def :todo/created-at #(instance? js/Date %))

(s/def :entity/todo
  (s/keys :req [:db/id :todo/title :todo/completed? :todo/created-at]))

(s/def :todomvc.store/app-db* d/conn?)
