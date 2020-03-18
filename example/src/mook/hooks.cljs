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

#_(defn wrap-with-mook-state-stores-context [props]
  (apply
    ((::r/provider state-stores-context) {:value stores*})
    (.-children props)))

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

(defn register-store! [store-key store]
  (assert (satisfies? core/IDeref store)
          "A mook store must implement the IDeref protocol")
  (assert (satisfies? Watchable store)
          "A mook store must implement the mook.hooks/Watchable protocol")
  (swap! stores* assoc store-key store))

(defn use-state-store [store-key handler]
  (let [state-store* (-> (use-mook-state-stores) (get store-key))]
    (assert state-store* (str "State store " store-key " does not exist."))
    (let [[value set-value!] (r/use-state #(handler @state-store*))
          last-value (r/use-ref value)]
      (r/use-effect (fn use-reactive-state-effect []
                      (let [sub-id (swap! listener-id-counter* inc)]
                        (add-watch state-store*
                                   sub-id
                                   (fn listen-to-store [new-state]
                                     (let [new-value (handler new-state)]
                                       (when (not= new-value (.-current last-value))
                                         (set! (.-current last-value))
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
