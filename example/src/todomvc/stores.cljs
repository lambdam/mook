(ns todomvc.stores
  (:require [clojure.spec.alpha :as s]
            [datascript.core :as d]
            [mook.core :as m]
            [todomvc.boundaries.ui :as b-ui]))

;; Specs

(s/def ::local-store
  map?)

(s/def ::local-store*
  #(satisfies? cljs.core/IAtom %))

(s/def ::app-db d/db?)

(s/def ::app-db* d/conn?)

(s/def ::state-stores
  (s/keys :req [::local-store* ::app-db*]))

(extend-type datascript.db/DB
  m/Watchable
  (m/listen! [this key f]
    (d/listen! this key (fn watch-changes [{:keys [db-after db-before] :as _transaction-data}]
                          (f {::new-state db-after
                              ::old-state db-before
                              }))))
  (m/unlisten! [this key]
    (d/unlisten! this key)))

(defonce app-db* (d/create-conn {}))

(m/register-store! ::app-db* app-db*)

(defonce local-store* (atom {::b-ui/active-filter :all
                             ;; :local/counter 0
                             }))
(m/register-store! ::local-store* local-store*)
