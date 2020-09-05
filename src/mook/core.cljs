(ns mook.core
  (:require [clojure.spec.alpha :as s]
            [mook.react :as mr]
            [promesa.core :as p])
  (:require-macros [mook.core :refer [dev-print!]]))

;; # Hooks and state stores

(defonce ^:private listener-id-counter*
  (atom 0))

(defonce ^:private stores*
  (atom {}))

(def ^:private state-stores-context
  (mr/create-context stores*))

(defn mook-state-store-container [el]
  (let [provider (::mr/provider state-stores-context)]
    (provider {:value stores*}
              el)))

(defn ^:private use-mook-state-stores []
  (-> state-stores-context
      ::mr/context
      mr/use-context
      deref))

(defprotocol Watchable
  (listen! [this key f])
  (unlisten! [this key]))

(extend-type Atom
  Watchable
  (listen! [this key f]
    (add-watch this key (fn watch-changes [_key _ref _old-state new-state]
                          (f {::new-state new-state
                              ;; ::old-state _old-state
                              ;; ::ref _ref
                              ;; ::key _key
                              }))))
  (unlisten! [this key]
    (remove-watch this key)))

(defn register-store! [store-key store]
  (assert (satisfies? IDeref store)
          (str "Error on store registration for: " store-key ". A mook store must implement the IDeref protocol"))
  (assert (satisfies? Watchable store)
          (str "Error on store registration for: " store-key ". A mook store must implement the mook.core/Watchable protocol"))
  (swap! stores* assoc store-key store))

(s/def ::store-key keyword?)
(s/def ::params any?)
(s/def ::handler ifn?)
(s/def ::debug-info any?)

(s/def ::new-state any?)
(s/def ::listener-data
  (s/keys :req [::new-state]))

(defn use-state-store
  ([{::keys [store-key handler debug-info]}]
   (let [state-store* (as-> (use-mook-state-stores) <>
                        (get <> store-key)
                        (do (assert <> (str "State store " store-key " does not exist."))
                            <>))
         [value set-value!] (mr/use-state #(let [value (handler @state-store*)]
                                             (dev-print! {:type :first-hook-call
                                                          :f 'use-state-store
                                                          :store-key store-key
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
                        (listen! state-store*
                                 sub-id
                                 (fn listen-to-store [{::keys [new-state] :as data}]
                                   (s/assert ::listener-data data)
                                   (let [handler' (.-current last-handler-ref)
                                         new-value (let [value (handler' new-state)]
                                                     (dev-print! {:type :watcher-call
                                                                  :f 'use-state-store
                                                                  :store-key store-key
                                                                  :handler handler
                                                                  :value value
                                                                  :debug-info debug-info}
                                                                 "red")
                                                     value)]
                                     (when (not= new-value (.-current last-value-ref))
                                       (dev-print! {:type :rerender-on-watcher-call
                                                    :f 'use-state-store
                                                    :store-key store-key
                                                    :handler handler
                                                    :old-value (.-current last-value-ref)
                                                    :new-value new-value
                                                    :debug-info debug-info}
                                                   "darkviolet")
                                       (set! (.-current last-value-ref) new-value)
                                       (set-value! new-value)))))
                        (fn remove-store-watch []
                          (unlisten! state-store* sub-id))))
                    #js [])
     (set! (.-current last-handler-ref) handler)
     (when (-> first-call?-ref .-current false?)
       (let [new-value (handler @state-store*)]
         (dev-print! {:type :check-new-value
                      :f 'use-state-store
                      :handler handler
                      :value new-value
                      :debug-info debug-info}
                     "green")
         ;; !!! Warning : this new value doesn't play well with JS objects
         (when (not= new-value (.-current last-value-ref))
           (dev-print! {:type :rerender-on-new-value
                        :f 'use-state-store
                        :handler handler
                        :old-value (.-current last-value-ref)
                        :new-value new-value
                        :debug-info debug-info}
                       "maroon")
           (set! (.-current last-value-ref) new-value)
           (set-value! new-value))))
     (set! (.-current first-call?-ref) false)
     value))
  ([store-key handler] (use-state-store {::store-key store-key
                                         ::handler  handler})))

(s/fdef use-state-store
  :args (s/alt :unary (s/keys :req [::store-key ::handler]
                              :opt [::debug-info])
               :binary (s/cat :store-key ::store-key
                              :handler ::handler))
  :ret any?)

;; ---

(defn use-param-state-store
  ([{::keys [store-key params handler debug-info]}]
   (let [state-store* (as-> (use-mook-state-stores) <>
                        (get <> store-key)
                        (do (assert <> (str "State store " store-key " does not exist."))
                            <>))
         [value set-value!] (mr/use-state #(let [value (handler @state-store*)]
                                             (dev-print! {:type :first-hook-call
                                                          :f 'use-param-state-store
                                                          :store-key store-key
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
                        (listen! state-store*
                                 sub-id
                                 (fn listen-to-store [{::keys [new-state] :as data}]
                                   (s/assert ::listener-data data)
                                   (let [handler' (.-current last-handler-ref)
                                         params' (.-current last-params-ref)
                                         new-value (let [value (handler' new-state)]
                                                     (dev-print! {:type :watcher-call
                                                                  :f 'use-param-state-store
                                                                  :store-key store-key
                                                                  :params params'
                                                                  :value value
                                                                  :debug-info debug-info}
                                                                 "red")
                                                     value)]
                                     (when (not= new-value (.-current last-value-ref))
                                       (dev-print! {:type :rerender-on-watcher-call
                                                    :f 'use-param-state-store
                                                    :store-key store-key
                                                    :params params'
                                                    :old-value (.-current last-value-ref)
                                                    :new-value new-value
                                                    :debug-info debug-info}
                                                   "darkviolet")
                                       (set! (.-current last-value-ref) new-value)
                                       (set-value! new-value)))))
                        (fn remove-store-watch []
                          (unlisten! state-store* sub-id))))
                    #js [])
     (when (not= params (.-current last-params-ref))
       (set! (.-current last-params-ref) params)
       (let [new-value (handler @state-store*)]
         (dev-print! {:type :check-new-value
                      :f 'use-param-state-store
                      :store-key store-key
                      :params params
                      :value new-value
                      :debug-info debug-info}
                     "green")
         (when (not= new-value (.-current last-value-ref))
           (dev-print! {:type :rerender-on-new-value
                        :f 'use-param-state-store
                        :store-key store-key
                        :params params
                        :old-value (.-current last-value-ref)
                        :new-value new-value
                        :debug-info debug-info}
                       "maroon")
           (set! (.-current last-value-ref) new-value)
           (set-value! new-value))))
     (set! (.-current last-handler-ref) handler)
     value))
  ([store-key params handler]
   (use-param-state-store {::store-key store-key
                           ::params params
                           ::handler handler})))

(s/fdef use-param-state-store
  :args (s/alt :unary (s/keys :req [::store-key ::params ::handler]
                              :opt [::debug-info])
               :ternary (s/cat :store-key ::store-key
                               :params ::params
                               :handler ::handler))
  :ret any?)

;; ---

;; Utility function in case we need some values on every component call
(defn run-on-state-stores [store-keys handler]
  (let [stores @stores*]
    (doseq [store-key store-keys]
      (assert (contains? stores store-key)
              (str "Try to run handler on non registered store: " store-key)))
    (->> store-keys
         (map #(-> (get stores %) deref))
         (apply handler))))

(s/fdef run-on-state-stores
  :args (s/cat :store-keys (s/coll-of ::store-key :kind vector?)
               :handler ifn?)
  :ret any?)


;; # Commands

(s/def ::type keyword?)
(s/def ::context
  (s/keys :opt [::type]))
(s/def ::input-context
  (s/keys :req [::type]))

(defn ^:private command-dispatch [{::keys [type]}]
  type)

(s/fdef command-dispatch
  :args (s/cat :context ::input-context)
  :ret ::type)

(defonce ^:private command-handlers*
  (atom {}))

(defn ^:private default-command-handler [{::keys [type] :as context}]
  (p/rejected (ex-info (str "No dispatch method for type: " type)
                       context)))

;; !!! using multimethod instead of an atom was throwing the following error on refresh with (st/instrument) "activated"

;; ExceptionsManager.js:82 Error: No protocol method IMultiFn.-add-method defined for type function: function (var_args){
;; var args = null;
;; if (arguments.length > 0) {
;; var G__91127__i = 0, G__91127__a = new Array(arguments.length -  0);
;; while (G__91127__i < G__91127__a.length) {G__91127__a[G__91127__i] = arguments[G__91127__i + 0]; ++G__91127__i;}
;;   args = new cljs.core.IndexedSeq(G__91127__a,0,null);
;; } 
;; return G__91124__delegate.call(this,args);}

;; I had this type of declarations

;; (defn-spec my-event ...
;;   [...])

;; (defmethod mook/command>> ::my-event [context]
;;   (my-event context))

(defn register-command! [key handler]
  (swap! command-handlers* assoc key handler)
  nil)

(s/fdef register-command!
  :args (s/cat :key keyword? :handler fn?)
  :ret nil?)

(defn send-command>> [context]
  (let [handler (or (->> (command-dispatch context) (get @command-handlers*))
                    default-command-handler)]
    (-> context
        (merge @stores*)
        handler
        (p/then #(dissoc % ::type)))))

(s/fdef send-command>>
  :args (s/cat :context ::input-context)
  :ret p/promise?)

(defn chain-command [context]
  (fn chain-command-clsr [prom]
    (p/chain
      prom
      #(send-command>> (merge % context)))))

(s/fdef chain-command
  :args (s/cat :context ::input-context)
  :ret fn?)

;; ---

(defn ^:private chain-context' [handler context]
  (-> (merge context @stores*)
      handler))

(s/fdef chain-context'
  :args (s/cat :handler fn? :context ::context)
  :ret (s/or :context ::context
             :promise p/promise?))

(defn chain-context [handler]
  (fn [context]
    (chain-context' handler context)))
