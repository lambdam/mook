(ns mook.core
  (:require [clojure.spec.alpha :as s]
            [mook.react :as mr]
            [promesa.core :as p])
  (:require-macros [mook.core :refer [dev-print!]]))

(defprotocol Watchable
  (listen! [this key f])
  (unlisten! [this key]))

(extend-type Atom
  Watchable
  (listen! [this key f]
    (add-watch this key (fn watch-changes [_key _ref _old-state new-state]
                          (f {::new-state new-state}))))
  (unlisten! [this key]
    (remove-watch this key)))

;; Commands and state stores

(s/def ::store-key keyword?)
(s/def ::state-key keyword?)
(s/def ::store* #(satisfies? IDeref %))
(s/def ::state any?)
(s/def ::new-state any?)
(s/def ::store-info (s/keys :req [::store-key ::state-key ::store*]))
(s/def ::store-infos (s/coll-of ::store-info))
(s/def ::state-transition (s/keys :req [::state-key ::new-state]))
(s/def ::state-transitions (s/coll-of ::state-transition))

(defonce ^:private store-key->store-info*
  (atom {}))

(defonce ^:private state-key->store-info*
  (atom {}))

(defn create-state-store-wrapper [store-infos]
  (doseq [{::keys [store-key state-key store*] :as store-info} store-infos]
    (assert (satisfies? IDeref store*)
            (str "Error on store registration for: " store-key ". A mook store must implement the IDeref protocol"))
    (assert (satisfies? Watchable store*)
            (str "Error on store registration for: " store-key ". A mook store must implement the mook.core/Watchable protocol"))
    (swap! store-key->store-info* assoc store-key store-info)
    (swap! state-key->store-info* assoc state-key store-info))
  (fn wrap-data-state-stores [command>>]
    (let [store-states (reduce (fn [acc store-info]
                                 (assoc acc
                                        ;; Store
                                        (::store-key store-info)
                                        (::store* store-info)
                                        ;; State
                                        (::state-key store-info)
                                        (-> store-info ::store* deref)))
                               {}
                               store-infos)]
      (fn process-data-state-stores>> [data]
        (-> (merge data store-states)
            command>>
            (p/then
              (fn [data']
                (as-> data' <>
                  ;; Handle declarative state transitions
                  (if-let [state-transitions (::state-transitions <>)]
                    (do (doseq [{::keys [state-key new-state]} state-transitions]
                          (-> (get @state-key->store-info* state-key)
                              ::store*
                              (reset! new-state)))
                        (dissoc <> ::state-transitions))
                    <>)
                  ;; Always set the latest version of the states
                  (reduce-kv (fn [acc _k store-info]
                               (assoc acc
                                      (::state-key store-info)
                                      (-> store-info ::store* deref)))
                             <>
                             @store-key->store-info*)))))))))

(s/fdef create-state-store-wrapper
  :args (s/cat :store-infos ::store-infos)
  :ret fn?)

;; ---

(def ^:private apply-middlewares*
  (atom identity))

(defn wrap [cmd]
  (fn wrapped-command [data]
    (as-> (@apply-middlewares* cmd) <>
      (<> data))))

(s/def ::command-middlewares
  (s/coll-of fn? :kind vector?))

(defn init-mook! [{::keys [command-middlewares] :as data}]
  (reset! apply-middlewares* (apply comp (reverse command-middlewares))))

(s/fdef init-mook!
  :args (s/cat :data (s/keys :req [::command-middlewares]))
  :ret any?)

;; Hooks

(s/def ::params any?)
(s/def ::handler ifn?)
(s/def ::debug-info any?)

(s/def ::listener-data
  (s/keys :req [::new-state]))

(defonce ^:private listener-id-counter*
  (atom 0))

(defn use-mook-state
  ([{::keys [state-key handler debug-info]}]
   (let [store* (as-> (get @state-key->store-info* state-key) <>
                  (::store* <>)
                  (do (assert <> (str "Mook state " state-key " does not exist."))
                      <>))
         [value set-value!] (mr/use-state #(let [value (handler @store*)]
                                             (dev-print! {:type :first-hook-call
                                                          :f 'use-mook-state
                                                          :state-key state-key
                                                          :handler handler
                                                          :value value
                                                          :debug-info debug-info}
                                                         "blue")
                                             value))
         first-call?-ref (mr/use-ref true)
         last-value-ref (mr/use-ref value)
         last-handler-ref (mr/use-ref handler)]
     (mr/use-effect (fn use-reactive-state-effect []
                      (let [sub-id (swap! listener-id-counter* inc)]
                        (listen! store*
                                 sub-id
                                 (fn listen-to-store [{::keys [new-state] :as data}]
                                   (s/assert ::listener-data data)
                                   (let [handler' (.-current last-handler-ref)
                                         new-value (let [value (handler' new-state)]
                                                     (dev-print! {:type :watcher-call
                                                                  :f 'use-mook-state
                                                                  :state-key state-key
                                                                  :handler handler
                                                                  :value value
                                                                  :debug-info debug-info}
                                                                 "red")
                                                     value)]
                                     (when (not= new-value (.-current last-value-ref))
                                       (dev-print! {:type :rerender-on-watcher-call
                                                    :f 'use-mook-state
                                                    :state-key state-key
                                                    :handler handler
                                                    :old-value (.-current last-value-ref)
                                                    :new-value new-value
                                                    :debug-info debug-info}
                                                   "darkviolet")
                                       (set! (.-current last-value-ref) new-value)
                                       (set-value! new-value)))))
                        (fn remove-store-watch []
                          (unlisten! store* sub-id))))
                    #js [])
     (set! (.-current last-handler-ref) handler)
     (when (-> first-call?-ref .-current false?)
       (let [new-value (handler @store*)]
         (dev-print! {:type :check-new-value
                      :f 'use-mook-state
                      :handler handler
                      :value new-value
                      :debug-info debug-info}
                     "green")
         ;; !!! Warning : this new value doesn't play well with JS objects
         (when (not= new-value (.-current last-value-ref))
           (dev-print! {:type :rerender-on-new-value
                        :f 'use-mook-state
                        :handler handler
                        :old-value (.-current last-value-ref)
                        :new-value new-value
                        :debug-info debug-info}
                       "maroon")
           (set! (.-current last-value-ref) new-value)
           ;; Fire a re-render
           (set-value! new-value))))
     (set! (.-current first-call?-ref) false)
     value))
  ([state-key handler] (use-mook-state {::state-key state-key
                                        ::handler  handler})))

(s/fdef use-mook-state
  :args (s/alt :unary (s/keys :req [::state-key ::handler]
                              :opt [::debug-info])
               :binary (s/cat :state-key ::state-key
                              :handler ::handler))
  :ret any?)

;; ---

(defn use-param-mook-state
  ([{::keys [state-key params handler debug-info]}]
   (let [store* (as-> (get @state-key->store-info* state-key) <>
                  (::store* <>)
                  (do (assert <> (str "Mook state " state-key " does not exist."))
                      <>))
         [value set-value!] (mr/use-state #(let [value (handler @store*)]
                                             (dev-print! {:type :first-hook-call
                                                          :f 'use-param-mook-state
                                                          :state-key state-key
                                                          :params params
                                                          :value value
                                                          :debug-info debug-info}
                                                         "blue")
                                             value))
         last-value-ref (mr/use-ref value)
         last-params-ref (mr/use-ref params)
         last-handler-ref (mr/use-ref handler)]
     (mr/use-effect (fn use-reactive-state-effect []
                      (let [sub-id (swap! listener-id-counter* inc)]
                        (listen! store*
                                 sub-id
                                 (fn listen-to-store [{::keys [new-state] :as data}]
                                   (s/assert ::listener-data data)
                                   (let [handler' (.-current last-handler-ref)
                                         params' (.-current last-params-ref)
                                         new-value (let [value (handler' new-state)]
                                                     (dev-print! {:type :watcher-call
                                                                  :f 'use-param-mook-state
                                                                  :state-key state-key
                                                                  :params params'
                                                                  :value value
                                                                  :debug-info debug-info}
                                                                 "red")
                                                     value)]
                                     (when (not= new-value (.-current last-value-ref))
                                       (dev-print! {:type :rerender-on-watcher-call
                                                    :f 'use-param-mook-state
                                                    :state-key state-key
                                                    :params params'
                                                    :old-value (.-current last-value-ref)
                                                    :new-value new-value
                                                    :debug-info debug-info}
                                                   "darkviolet")
                                       (set! (.-current last-value-ref) new-value)
                                       (set-value! new-value)))))
                        (fn remove-store-watch []
                          (unlisten! store* sub-id))))
                    #js [])
     (when (not= params (.-current last-params-ref))
       (set! (.-current last-params-ref) params)
       (let [new-value (handler @store*)]
         (dev-print! {:type :check-new-value
                      :f 'use-param-mook-state
                      :state-key state-key
                      :params params
                      :value new-value
                      :debug-info debug-info}
                     "green")
         (when (not= new-value (.-current last-value-ref))
           (dev-print! {:type :rerender-on-new-value
                        :f 'use-param-mook-state
                        :state-key state-key
                        :params params
                        :old-value (.-current last-value-ref)
                        :new-value new-value
                        :debug-info debug-info}
                       "maroon")
           (set! (.-current last-value-ref) new-value)
           (set-value! new-value))))
     (set! (.-current last-handler-ref) handler)
     value))
  ([state-key params handler]
   (use-param-mook-state {::state-key state-key
                          ::params params
                          ::handler handler})))

(s/fdef use-param-mook-state
  :args (s/alt :unary (s/keys :req [::state-key ::params ::handler]
                              :opt [::debug-info])
               :ternary (s/cat :state-key ::state-key
                               :params ::params
                               :handler ::handler))
  :ret any?)
