(ns todomvc.boundaries.todo
  (:require [clojure.spec.alpha :as s]))

(s/def :db/id integer?)

(s/def :todo/title string?)
(s/def :todo/completed? boolean?)
(s/def :todo/created-at #(instance? js/Date %))

(s/def ::todo
  (s/keys :req [:db/id :todo/title :todo/completed? :todo/created-at]))
