(ns mook.hooks
  (:refer-clojure :exclude [add-watch remove-watch compare])
  (:require [mook.react :as r]
            [clojure.core :as core]))

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

(defonce print?* (atom true))

(defn dev-print! [msg store-key handler color]
  (when @print?*
    (js/console.log (clojure.string/join "\n" [(str "%c" msg) (str store-key) (-> handler print with-out-str)])
                    (str "background: " color "; color: white"))))

(defn register-store! [store-key store]
  (assert (satisfies? core/IDeref store)
          "A mook store must implement the IDeref protocol")
  (assert (satisfies? Watchable store)
          "A mook store must implement the mook.hooks/Watchable protocol")
  (swap! stores* assoc store-key store))

(defn use-state-store [store-key handler]
  (dev-print! "use-state-store main" store-key handler "green")
  (let [state-store* (as-> (use-mook-state-stores) <>
                       (get <> store-key)
                       (do (assert <> (str "State store " store-key " does not exist."))
                           <>))
        [value set-value!] (r/use-state #(do (dev-print! "use-state-store hook" store-key handler "maroon")
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
                                         new-value #(do (dev-print! "use-state-store store watcher" store-key handler "red")
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
          (dev-print! "use-state-store new handler value" store-key handler "blue")
          (set! (.-current last-value-ref) new-value)
          (set-value! new-value))))
    (set! (.-current first-call?-ref) false)
    value))

(defn use-keyed-state-store [store-key {::keys [params handler]}]
  (dev-print! "use-keyed-state-store main" store-key params "green")
  (let [state-store* (as-> (use-mook-state-stores) <>
                       (get <> store-key)
                       (do (assert <> (str "State store " store-key " does not exist."))
                           <>))
        [value set-value!] (r/use-state #(do (dev-print! "use-state-store hook" store-key params "maroon")
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
                                         new-value #(do (dev-print! "use-keyed-state-store store watcher" store-key params "red")
                                                        (handler' new-state))]
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
          (dev-print! "use-keyed-state-store new params value" store-key params "blue")
          (set! (.-current last-value-ref) new-value)
          (set-value! new-value))))
    (set! (.-current last-handler-ref) handler)
    value))

(defn use-cached-state-store [store-key {::keys [id params handler]}]
  (dev-print! "use-cached-state-store main" store-key params "green")
  (let [state-store* (as-> (use-mook-state-stores) <>
                       (get <> store-key)
                       (do (assert <> (str "State store " store-key " does not exist."))
                           <>))
        [value set-value!] (r/use-state (fn []
                                          (dev-print! "use-cached-state-store hook" store-key params "maroon")
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
                                         new-value #(do (dev-print! "use-cached-state-store store watcher" store-key params "red")
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
          (dev-print! "use-cached-state-store new params value" store-key params "blue")
          (swap! cache* update-in [id ::value] new-value)
          (set-value! new-value))))
    (set! (.-current last-handler-ref) handler)
    value))
