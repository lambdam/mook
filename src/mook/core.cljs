(ns mook.core
  (:refer-clojure :exclude [add-watch remove-watch])
  (:require [cljs.spec.alpha :as s]
            [clojure.core :as core]
            [mook.react :as r]
            [promesa.core :as p]))

;; # Hooks

(defonce listener-id-counter*
  (atom 0))

(defonce cache*
  (atom {}))

(defonce stores*
  (atom {}))

(def state-stores-context
  (r/create-context stores*))

(defn wrap-with-mook-state-stores-context [root-component]
  (let [provider (::r/provider state-stores-context)]
    (provider {:value stores*}
      (r/create-element root-component))))

(defn use-mook-state-stores []
  (-> state-stores-context
      ::r/context
      r/use-context
      deref))

(defprotocol Watchable
  (add-watch [this key f])
  (remove-watch [this key]))

(extend-type core/Atom
  Watchable
  (add-watch [this key f]
    (core/add-watch this key (fn watch-changes [_key _atom _old-value new-value]
                               (f new-value))))
  (remove-watch [this key]
    (core/remove-watch this key)))

#_(defonce print?* (atom true))

#_(defn dev-print! [data color]
  (when @print?*
    (js/console.log (str "%c" (-> data cljs.pprint/pprint with-out-str))
                    (str "background: " color "; color: white"))))

(defn register-store! [store-key store]
  (assert (satisfies? core/IDeref store)
          "A mook store must implement the IDeref protocol")
  (assert (satisfies? Watchable store)
          "A mook store must implement the mook.core/Watchable protocol")
  (swap! stores* assoc store-key store))

(defn use-state-store [store-key handler]
  (let [state-store* (as-> (use-mook-state-stores) <>
                       (get <> store-key)
                       (do (assert <> (str "State store " store-key " does not exist."))
                           <>))
        #_#__ (dev-print! {:type :every-call
                       :f 'use-state-store
                       :store-key store-key
                       :handler handler
                       :value (handler @state-store*)}
                      "green")
        [value set-value!] (r/use-state #(do #_(dev-print! {:type :first-hook-call
                                                          :f 'use-state-store
                                                          :store-key store-key
                                                          :handler handler
                                                          :value (handler @state-store*)}
                                                         "maroon")
                                             (handler @state-store*)))
        first-call?-ref (r/use-ref true)
        last-value-ref (r/use-ref value)
        last-handler-ref (r/use-ref handler)]
    (r/use-effect (fn use-reactive-state-effect []
                    (let [sub-id (swap! listener-id-counter* inc)]
                      (add-watch state-store*
                                 sub-id
                                 (fn listen-to-store [new-state]
                                   (let [handler' (.-current last-handler-ref)
                                         new-value #(do #_(dev-print! {:type :watcher-call
                                                                     :f 'use-state-store
                                                                     :store-key store-key
                                                                     :handler handler
                                                                     :value (handler' new-state)}
                                                                    "red")
                                                        (handler' new-state))]
                                     (when (not= new-value (.-current last-value-ref))
                                       (set! (.-current last-value-ref) new-value)
                                       (set-value! new-value)))))
                      (fn remove-store-watch []
                        (remove-watch state-store* sub-id))))
                  #js [])
    (set! (.-current last-handler-ref) handler)
    (when (-> first-call?-ref .-current false?)
      (let [new-value (handler @state-store*)]
        (when (not= new-value (.-current last-value-ref))
          #_(dev-print! {:type :new-value
                       :f 'use-state-store
                       :handler handler
                       :value new-value}
                      "blue")
          (set! (.-current last-value-ref) new-value)
          (set-value! new-value))))
    (set! (.-current first-call?-ref) false)
    value))

(defn use-keyed-state-store [store-key {::keys [params handler]}]
  (let [state-store* (as-> (use-mook-state-stores) <>
                       (get <> store-key)
                       (do (assert <> (str "State store " store-key " does not exist."))
                           <>))
        #_#__ (dev-print! {:type :every-call
                       :f 'use-keyed-state-store
                       :store-key store-key
                       :params params
                       :value (handler @state-store* params)}
                      "green")
        [value set-value!] (r/use-state #(do #_(dev-print! {:type :first-hook-call
                                                          :f 'use-keyed-state-store
                                                          :store-key store-key
                                                          :params params
                                                          :value (handler @state-store* params)}
                                                         "maroon")
                                             (handler @state-store* params)))
        last-value-ref (r/use-ref value)
        last-params-ref (r/use-ref params)
        last-handler-ref (r/use-ref handler)]
    (r/use-effect (fn use-reactive-state-effect []
                    (let [sub-id (swap! listener-id-counter* inc)]
                      (add-watch state-store*
                                 sub-id
                                 (fn listen-to-store [new-state]
                                   (let [handler' (.-current last-handler-ref)
                                         params' (.-current last-params-ref)
                                         new-value #(do #_(dev-print! {:type :watcher-call
                                                                     :f 'use-keyed-state-store
                                                                     :store-key store-key
                                                                     :params params'
                                                                     :value (handler' new-state params')}
                                                                    "red")
                                                        (handler' new-state params'))]
                                     (when (not= new-value (.-current last-value-ref))
                                       (set! (.-current last-value-ref) new-value)
                                       (set-value! new-value)))))
                      (fn remove-store-watch []
                        (remove-watch state-store* sub-id))))
                  #js [])
    (when (not= params (.-current last-params-ref))
      (set! (.-current last-params-ref) params)
      (let [new-value (handler @state-store* params)]
        (when (not= new-value (.-current last-value-ref))
          #_(dev-print! {:type :new-value
                       :f 'use-keyed-state-store
                       :store-key store-key
                       :params params
                       :value new-value}
                      "blue")
          (set! (.-current last-value-ref) new-value)
          (set-value! new-value))))
    (set! (.-current last-handler-ref) handler)
    value))

(defn use-cached-state-store [store-key {::keys [id params handler]}]
  (let [state-store* (as-> (use-mook-state-stores) <>
                       (get <> store-key)
                       (do (assert <> (str "State store " store-key " does not exist."))
                           <>))
        #_#__ (dev-print! {:type :every-call
                       :f 'use-cached-state-store
                       :store-key store-key
                       :params params
                       :value (handler @state-store* params)}
                      "green")
        [value set-value!] (r/use-state (fn []
                                          #_(dev-print! {:type :first-hook-call
                                                       :f 'use-cached-state-store
                                                       :store-key store-key
                                                       :params params
                                                       :value (handler @state-store* params)}
                                                      "maroon")
                                          (let [store @state-store*
                                                cache @cache*]
                                            (if (and (contains? cache id)
                                                     (identical? store (get-in cache [id ::store]))
                                                     (= params (get-in cache [id ::params])))
                                              (get-in @cache* [id ::value])
                                              (let [value (handler @state-store* params)]
                                                (swap! cache* update id #(do {::value value
                                                                              ::params params
                                                                              ::store store}))
                                                value)))))
        last-handler-ref (r/use-ref handler)]
    (r/use-effect (fn use-reactive-state-effect []
                    (let [sub-id (swap! listener-id-counter* inc)]
                      (add-watch state-store*
                                 sub-id
                                 (fn listen-to-store [new-state]
                                   (swap! cache* update-in [id ::store] #(do new-state))
                                   (let [handler' (.-current last-handler-ref)
                                         cache @cache*
                                         new-value #(do #_(dev-print! {:type :watcher-call
                                                                     :f 'use-keyed-state-store
                                                                     :store-key store-key
                                                                     :params params
                                                                     :value (handler' new-state (get-in cache [id ::params]))}
                                                                    "red")
                                                        (handler' new-state (get-in cache [id ::params])))]
                                     (when (not= new-value (get-in cache [id ::value]))
                                       (swap! cache* update-in [id ::value] #(do new-value))
                                       (set-value! new-value)))))
                      (fn remove-store-watch []
                        (remove-watch state-store* sub-id))))
                  #js [])
    (when (not= params (get-in @cache* [id ::params]))
      (swap! cache* update-in [id ::params] params)
      (let [new-value (handler @state-store* params)]
        (when (not= new-value (get-in @cache* [id ::value]))
          #_(dev-print! {:type :new-value
                       :f use-cached-state-store
                       :store-key store-key
                       :params params
                       :value new-value}
                      "blue")
          (swap! cache* update-in [id ::value] new-value)
          (set-value! new-value))))
    (set! (.-current last-handler-ref) handler)
    value))

;; # Commands

(defn command-dispatch [[type _data :as _event] ]
  type)

(s/fdef command-dispatch
  :args (s/cat :event (s/and vector?
                             (s/cat :event-type keyword? :data map?)))
  :ret keyword?)

(defmulti command>> command-dispatch)

(s/fdef command>>
  :args (s/cat :data map?)
  :ret p/promise?)

(defmethod command>> :default [[type _ :as event]]
  (p/rejected (ex-info (str "No dispatch method for type: " type)
                       event)))
