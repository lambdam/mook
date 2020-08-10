(ns mook.core
  (:require [clojure.spec.alpha :as s]
            [mook.react :as mr]
            [promesa.core :as p])
  #?(:cljs
     (:require-macros [mook.core :refer [dev-print!]])))

#?(:clj
   (do

     (def color-log? false)

     (defmacro dev-print! [data color]
       (when color-log?
         `(js/console.log (str "%c" (-> ~data cljs.pprint/pprint with-out-str))
                          (str "background: " ~color "; color: white"))))

     ))

#?(:cljs
   (do

     ;; # Hooks

     (defonce listener-id-counter*
       (atom 0))

     (defonce cache*
       (atom {}))

     (defonce stores*
       (atom {}))

     (def state-stores-context
       (mr/create-context stores*))

     (defn mook-state-store-container [el]
       (let [provider (::mr/provider state-stores-context)]
         (provider {:value stores*}
                   el)))

     (defn use-mook-state-stores []
       (-> state-stores-context
           ::mr/context
           mr/use-context
           deref))

     (defprotocol Watchable
       (listen! [this key f])
       (unlisten! [this key]))

     (s/def ::new-state any?)
     (s/def ::listener-data
       (s/keys :req [::new-state]))

     (s/def ::id (s/or :keyword keyword? :string string?))
     (s/def ::store-key keyword?)
     (s/def ::params (s/map-of keyword? any?))
     (s/def ::handler ifn?)

     (extend-type Atom
       Watchable
       (listen! [this key f]
         (add-watch this key (fn watch-changes [key ref old-state new-state]
                               (f {::new-state new-state
                                   ::old-state old-state
                                   ::ref ref
                                   ::key key}))))
       (unlisten! [this key]
         (remove-watch this key)))

     (defn register-store! [store-key store]
       (assert (satisfies? IDeref store)
               (str "Error on store registration for: " store-key ". A mook store must implement the IDeref protocol"))
       (assert (satisfies? Watchable store)
               (str "Error on store registration for: " store-key ". A mook store must implement the mook.core/Watchable protocol"))
       (swap! stores* assoc store-key store))

     ;; TODO: check if there can be an edge case where a store update but its
     ;; hanlder closes over an old value and update a wrong state.
     (defn use-state-store
       ([store-key handler] (use-state-store store-key handler nil))
       ([store-key handler debug-info]
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
          value)))

     (s/fdef use-state-store
       :args (s/cat :store-key keyword?
                    :handler ::handler
                    :debug (s/? any?))
       :ret any?)

     ;; ---

     (defn use-param-state-store
       ([store-key data]
        (use-param-state-store store-key data nil))
       ([store-key {::keys [params handler]} debug-info]
        (let [state-store* (as-> (use-mook-state-stores) <>
                             (get <> store-key)
                             (do (assert <> (str "State store " store-key " does not exist."))
                                 <>))
              [value set-value!] (mr/use-state #(let [value (handler @state-store* params)]
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
                                              new-value (let [value (handler' new-state params')]
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
            (let [new-value (handler @state-store* params)]
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
          value)))

     (s/fdef use-param-state-store
       :args (s/cat :store-key keyword?
                    :data (s/keys :req [::params ::handler])
                    :debug (s/? any?))
       :ret any?)

     ;; ---

     (defn use-cached-state-store
       ([store-key data]
        (use-cached-state-store store-key data nil))
       ([store-key {::keys [id params handler]} debug-info]
        (let [state-store* (as-> (use-mook-state-stores) <>
                             (get <> store-key)
                             (do (assert <> (str "State store " store-key " does not exist."))
                                 <>))
              [value set-value!] (mr/use-state (fn []
                                                 (let [store @state-store*
                                                       cache @cache*]
                                                   (if (and (contains? cache id)
                                                            (identical? store (get-in cache [id ::store]))
                                                            (= params (get-in cache [id ::params])))
                                                     (get-in @cache* [id ::value])
                                                     (let [value (handler @state-store* params)]
                                                       (dev-print! {:type :first-hook-call
                                                                    :f 'use-cached-state-store
                                                                    :store-key store-key
                                                                    :id id
                                                                    :params params
                                                                    :value value
                                                                    :debug-info debug-info}
                                                                   "blue")
                                                       (swap! cache* update id #(do {::id id
                                                                                     ::value value
                                                                                     ::params params
                                                                                     ::store store
                                                                                     ::store-key store-key}))
                                                       value)))))
              last-handler-ref (mr/use-ref handler)]
          (mr/use-effect (fn use-reactive-state-effect []
                           (let [sub-id (swap! listener-id-counter* inc)]
                             (listen! state-store*
                                      sub-id
                                      (fn listen-to-store [{::keys [new-state] :as data}]
                                        (s/assert ::listener-data data)
                                        (swap! cache* update-in [id ::store] #(do new-state))
                                        (let [handler' (.-current last-handler-ref)
                                              cache @cache*
                                              new-value (let [value (handler' new-state (get-in cache [id ::params]))]
                                                          (dev-print! {:type :watcher-call
                                                                       :f 'use-cached-state-store
                                                                       :store-key store-key
                                                                       :id id
                                                                       :params params
                                                                       :value value
                                                                       :debug-info debug-info}
                                                                      "red")
                                                          value)]
                                          (when (not= new-value (get-in cache [id ::value]))
                                            (dev-print! {:type :rerender-on-watcher-call
                                                         :f 'use-cached-state-store
                                                         :store-key store-key
                                                         :id id
                                                         :params params
                                                         :old-value (get-in cache [id ::value])
                                                         :new-value new-value
                                                         :debug-info debug-info}
                                                        "darkviolet")
                                            (swap! cache* update-in [id ::value] #(do new-value))
                                            (set-value! new-value)))))
                             (fn remove-store-watch []
                               (unlisten! state-store* sub-id))))
                         #js [])
          (when (not= params (get-in @cache* [id ::params]))
            (swap! cache* update-in [id ::params] params)
            (let [new-value (handler @state-store* params)]
              (dev-print! {:type :check-new-value
                           :f 'use-cached-state-store
                           :store-key store-key
                           :id id
                           :params params
                           :value new-value
                           :debug-info debug-info}
                          "green")
              (when (not= new-value (get-in @cache* [id ::value]))
                (dev-print! {:type :rerender-on-new-value
                             :f 'use-cached-state-store
                             :store-key store-key
                             :id id
                             :params params
                             :old-value (get-in @cache* [id ::value])
                             :new-value new-value
                             :debug-info debug-info}
                            "maroon")
                (swap! cache* update-in [id ::value] new-value)
                (set-value! new-value))))
          (set! (.-current last-handler-ref) handler)
          value)))

     (s/fdef use-cached-state-store
       :args (s/cat :store-key keyword?
                    :data (s/keys :req [::id ::params ::handler])
                    :debug (s/? any?))
       :ret any?)

     ;; # Commands

     (s/def ::type keyword?)
     (s/def ::context
       (s/keys :opt [::type]))
     (s/def ::input-context
       (s/keys :req [::type]))

     ;; On command error
     (s/def ::cmd-error-type keyword?)

     (defn command-dispatch [{::keys [type]}]
       type)

     (s/fdef command-dispatch
       :args (s/cat :context ::input-context)
       :ret ::type)

     (defonce ^:private command-handlers*
       (atom {}))

     (defn ^:private default-command-handler [{::keys [type] :as context}]
       (p/rejected (ex-info (str "No dispatch method for type: " type)
                            context)))

     ;; !!! using multimethod would throw the following error on refresh with (st/instrument) "activated"

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

     (s/fdef chain-command>>
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

     ;; ---

     (defn run-on-state-stores [store-keys handler]
       (let [stores @stores*]
         (doseq [store-key store-keys]
           (assert (contains? stores store-key)
                   (str "Try to run handler on non registered store: " store-key)))
         (->> store-keys
              (map #(-> (get stores %) deref))
              (apply handler))))

     (s/fdef run-on-state-stores
       :args (s/cat :store-keys (s/coll-of keyword? :kind vector?)
                    :handler ifn?)
       :ret any?)

     ))