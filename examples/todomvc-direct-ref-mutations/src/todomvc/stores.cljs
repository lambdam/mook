(ns todomvc.stores
  (:require [clojure.spec.alpha :as s]
            [datascript.core :as d]
            [mook.core :as m]
            [todomvc.boundaries.ui :as b-ui]))

(extend-type datascript.db/DB
  m/Watchable
  (m/listen! [this key f]
    (d/listen! this key (fn watch-changes [{:keys [db-after] :as _transaction-data}]
                          (f {::m/new-state db-after}))))
  (m/unlisten! [this key]
    (d/unlisten! this key)))

;; ---

(s/def ::local-store map?)
(s/def ::local-store* #(satisfies? cljs.core/IAtom %))

(defonce app-db* (d/create-conn {}))

;; ---

(s/def ::app-db d/db?)
(s/def ::app-db* d/conn?)

(defonce local-store* (atom {::b-ui/active-filter :all
                             ;; :local/counter 0
                             }))

;; ---

(s/def ::stores*
  (s/keys :req [::local-store* ::app-db*]))

(s/def ::states
  (s/keys :req [::local-store ::app-db]))
