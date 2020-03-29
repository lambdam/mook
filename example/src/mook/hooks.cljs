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

(defn use-state-store [store-key handler & args]
  (dev-print! "use-state-store main" store-key handler "green")
  (let [state-store* (-> (use-mook-state-stores) (get store-key))]
    (assert state-store* (str "State store " store-key " does not exist."))
    (let [[value set-value!] (r/use-state #(do (dev-print! "use-state-store hook" store-key handler "maroon")
                                               (handler @state-store*)))
          last-value (r/use-ref value)
          last-handler (r/use-ref handler)]
      (let [new-value (handler @state-store*)]
        (when (not= new-value (.-current last-value))
          (dev-print! "use-state-store new handler value" store-key handler "blue")
          (set! (.-current last-value)new-value)
          (set-value! new-value)))
      (set! (.-current last-handler) handler)
      (r/use-effect (fn use-reactive-state-effect []
                      (let [sub-id (swap! listener-id-counter* inc)]
                        (add-watch state-store*
                                   sub-id
                                   (fn listen-to-store [new-state]
                                     (let [handler' (.-current last-handler)
                                           new-value #(do (dev-print! "use-state-store store watcher" store-key handler "red")
                                                          (handler' new-state))]
                                       (when (not= new-value (.-current last-value))
                                         (set! (.-current last-value) new-value)
                                         (set-value! new-value)))))
                        (fn remove-store-watch []
                          (remove-watch state-store* sub-id))))
                    #js [])
      value)))

(defn use-cached-state-store [store-key {::keys [id params handler]}]
  (let [state-store* (-> (use-mook-state-stores) (get store-key))]
    (assert state-store* (str "State store " store-key " does not exist."))
    (let [[value set-value!] (r/use-state (fn [] ;; What happens when params changes ?
                                            (let [store @state-store*
                                                  previous-store (get-in @cache* [id :mook.cache/store])]
                                              (if (and (or (nil? params)
                                                           (= params (get-in @cache* [id :mook.cache/params])))
                                                       previous-store
                                                       (identical? previous-store store))
                                                ;; identical store and same params => same value
                                                (get-in @cache* [id :mook.cache/value])
                                                ;; new value to cache
                                                (let [new-value (handler (assoc params :mook/store @state-store*))]
                                                  (swap! cache* assoc id #:mook.cache{:store store
                                                                                      :params params
                                                                                      :value new-value})
                                                  new-value)))))]
      (r/use-effect (fn []
                      (let [sub-id (swap! listener-id-counter* inc)]
                        (add-watch state-store*
                                   sub-id
                                   (fn [new-state]
                                     (let [last-value (get-in @cache* [id :mook.cache/value])
                                           new-value (handler (assoc params :mook/store new-state))]
                                       (if (= new-value last-value)
                                         (swap! cache* assoc-in [id :mook.cache/store] new-state)
                                         (do (swap! cache* update merge :mook.cache{:store new-state
                                                                                    :value new-value})
                                             (set-value! new-value))))))
                        (fn []
                          (remove-watch state-store* sub-id))))
                    ;; ??? Mettre params ???
                    #js [])
      value)))
