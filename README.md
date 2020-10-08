# Mook

[![Clojars Project](https://img.shields.io/clojars/v/mook.svg)](https://clojars.org/mook)

Mook is a library designed to handle frontend application state(s).  
It serves the same purpose than [re-frame](https://github.com/Day8/re-frame) or
[citrus](https://github.com/clj-commons/citrus).

⚠️**This library is in an experimental state. Depending on the feedbacks that I
would receive in the coming weeks, things can change.**  
But that's the point: play with the code and send feebacks (Clojurians Slack
"mook" channel or pull requests)! Check the [TodoMVC examples](/examples).

Also, check the [interactive article](https://lambdam.com/blog/2020-10-mook-bis/)
that introduces the library and explains the design decisions.

```clj
;; Clojure CLI/deps.edn
mook {:mvn/version "0.2.0"}

;; Leiningen/Boot
[mook "0.2.0"]
```


## Design ideas

- Query the state directly and locally in components.
- Use functions and promises to handle state transitions and async workflows (aka
  actions, or commands in Mook parlance).
- Use Ring style middlewares to extend command (~ action) behaviors.
- Make commands (~ actions) composable.
- Enable the use of Datascript along with atoms to store state.


## The state and its transformation

Traditionally, mutating the global state is done through "actions". I chose
another semantics after a discussion with a friend
([@chpill](https://github.com/chpill)): "commands". The semantics is taken from
the event sourcing architecture that distinguishes "facts", things that happened
for sure, and "commands", sending the intention of a transformation. But this
command can fail for many reasons.

A **command** in mook is a **function that takes a map and returns a promise
that resolves to a map**. The promise expresses the fact that the future result
of a command can be a success or a failure. Also the promise has the useful
property to be chainable.

This is very similar to an async Ring handler that returns a
[Manifold](https://github.com/aleph-io/manifold)
[deferred](https://aleph.io/manifold/deferreds.html) (used with the
[Aleph](https://github.com/aleph-io/aleph) webserver). And the traditional
way of extending handlers in Ring, is to use
**[middlewares](https://github.com/ring-clojure/ring/wiki/Concepts#middleware)**.


## Usage

Mook introduces the notion of state stores: instead of having one source of
state that would fire global re-renders on every little change, it enables
having smaller pieces of state that would fire partial re-renders.

Typically two types of state stores can be used:
- A Datascript database that would hold state that flows troughout all the
  architecture (frontend and backend)
- A Clojure atom that would hold frontend only state and that acts like a
  lightweight key-value store.

```cljs
;; Classical one source of truth hashmap
{:foo ...  ;; <- any change in the hashmap will fire a whole re-render.
 :bar ...}

;; Mook approach with state stores
{:local-store {...}             ;; <- changes to local-store re-render only concerned UI parts
 :app-db <Datascript db value>} ;; <- changes to app-db re-render only concerned UI parts
```

This is an optimization meant to fire re-renders only by store. It is useful for
complex Datascript queries that can be costly on every re-render.

This optimization is variation around the "one source of thruth" concept since
every state store is held in one big hashmap with keys corresponding to the
state store name.

### Setup

Now that we saw (briefly) what state stores, commands and middlewares are in
Mook context, let's glue them together.

Mook behavior and storage are configured through middlewares.  
There is one mandatory middleware to provide on initialization: the state stores
middleware.  
Then any other middleware can be added (for http requests, browser local storage
etc...).

Example:

```cljs
(ns my.app)

(require '[mook.core :as m])
(require '[promesa.core :as p])

;; Datascript (structured business logic)

(def db-schema {...})

(defonce app-db*
  (d/create-conn db-schema))

;; Atom (lightweight store)

(defonce local-store*
  {::current-user-id nil
   ::in-progress? false
   ...})

;; State stores middleware. Mandatory!
(def wrap-ref-state-stores
  (m/create-state-store-wrapper!
    [{::m/store-key ::local-store*
      ::m/state-key ::local-store
      ::m/store*    local-store*}
     {::m/store-key ::app-db*
      ::m/state-key ::app-db
      ::m/store*    app-db*}]))

;; Logging middleware, for the example
(defn wrap-console-log [command]
  (fn process-console-log>> [data]
    (println "Data before\n" data)
    (-> (command data)
        (p/then (fn [data']
                  (println "Data after\n" data')
                  data')))))

(m/init-mook!
  {::m/command-middlewares [wrap-ref-state-stores
                            wrap-console-log
                            ;; Add as many middlewares as you wish.
                            ;; They will be applied in the declared order.
                            ]})
```

⚠️ Notice how map keys are all namespaced. Mook heavilly uses core.spec and
defines specs for almost every value that flows through the architecture.  
Three values have noticeable semantics:
- `:mook.core/store*`: the store itself as a reference (the Clojure atom, the
  Datascript "connection"...).
- `:mook.core/store-key`: the name given to the store.
- `:mook.core/state-key`: the name of the state contained in a store (~ the
  dereferenced reference).

Finally we can launch our React application:

```cljs
(defn root-component [_props]
  ...)

(js/ReactDOM.render
  (js/React.createElement root-component nil)
  (js/document.getElementById "app-root"))
```

__Note: for the time being, Mook stores the state in a singleton. We don't have
to use React context to expose the stores.__

Mook is only about state management. But Mook relies on the Hooks API (React >=
16.8).  
For the view you can use:
- The bare React library.
- The very thin wrapper included in Mook. Check the
  [documentation](/doc/react-wrapper.md).
- Another hook ready wrapper like [helix](https://github.com/lilactown/helix) or
  [crinkle](https://github.com/favila/crinkle) (not tested yet).

Now, there are two things that we can do with our application: read data from
the state stores and modify the state stores.

### Read the state(s)

Mook defines two hooks: `use-mook-state` and `use-param-mook-state`.

Mook hooks have two arities: the unary one that accepts a map with all
parameters explicitly given. In a way, this arity acts like labelled arguments
in other languages (like OCaml for example). Respectively the binary and ternary
arities with positional arguments act like shorthand versions of the function
call.

`use-mook-state` takes a state store name and a handler. The handler receives
the dereferenced store (~ the state) as its first and only parameter. There are
only two ways for this hook to fire a re-render:
1. the result of the handler changes (the handler might close over changing
values)
2. the state store changes and the result of the previous known handler changes.

```cljs
(require '[mook.core :as m])

;; Arity 1
(use-mook-state {::m/state-key ::local-store
                 ::m/handler (fn [state]
                               (::current-user-id state))})

;; Arity 2 (shorthand)
(use-mook-state ::db* ::current-user-id)
```

A more evolved one (`use-param-mook-state`), similar to React behaviour with
component `key` attibute, where the developper controls the data that will
provoque a new comparison. This hook was crafted to address the fact that
complex queries in Datascript might be slow, and we don't want it to replay on
every functional component call. Also this hook fires a re-render when the "key"
value changes or that the result of a new state of the store changes.

```cljs
(require '[mook.core :as m])

;; Arity 1
(use-param-mook-state {::m/state-key ::app-db
                       ::m/params [current-user-id book-ids]
                       ::m/handler (fn [db] ...)})

;; Arity 3 (shorthand)
(use-param-mook-state ::app-db
                      [current-user-id book-ids]
                      (fn [db] ...))
```

### Mook commands (~ actions)

⚠️For the time being and since Mook is in an early stage, there are two ways of
transforming the states:
- By accessing the store references directly and transforming then directly in
  the commands.  
  This is the original Mook approach.
- By declaring a new value of a given store in the returned value of a command.  
  This approach is based on the feebacks from
  [@vvvvalvalval](https://github.com/vvvvalvalval) that favors considering the
  state as big immutable value without sacrificing the optimization of partial
  re-renders of the state (the state stores).

To access mook store context and behaviors defined in the middlewares, a command
has to be wrapped with mook middlewares.

This can be done statically in a namespace or dynamically in a React handlers.

```cljs
(require '[mook.core :as m])
(require '[promesa.core :as p])


;; The command
(defn create-new-todo>> [data]
  ...)

;; We can spec it! It is a regular function.
(s/fdef create-new-todo>>
  :args (s/cat :data ...)
  :ret p/promise?)

;; Finally we wrap it so that it will receive the stores in its
;; parameters (and any other thing defined in the middlewares).
(def <set-route>>
  (m/wrap set-route>>))
```

Notice the convention here. `...>>` indicates that the function returns a
promise. `<...>` indicated that the function has been wrapped with Mook
middlewares.

### State store middleware, version 1: direct reference access

The state store middleware merges all the states and stores in the data provided
to a command. In our case, for the input: `{:foo "bar"}`, the command will
receive the following map:

```cljs
{:foo "bar"
 ::local-store {...}
 ::local-store* <Atom ...>
 ::app-db #datascript/DB{...}
 ::app-db* <DB connection ...>
 }
```

This would be a command definition:

```cljs
(require '[mook.core :as m])
(require '[datascript.core :as d])
(require '[promesa.core :as p])

;; The command
(defn create-new-todo>> [{::keys [app-db* local-store*] :as data}]
  (let [title (:todo/title data)]
    (d/transact! app-db*
                 [{:todo/title title
                   :todo/completed? false
                   :todo/created-at (js/Date.)}])
    (swap! local-store* assoc ::latest-todo title)
    (p/resolved (dissoc data :todo/title))))

(defn set-route>> [{::keys [local-store*] :as data}]
  (swap! local-store* merge (select-keys data [::current-route]))
  (p/resolved (dissoc data ::current-route)))

;; We can spec it! It is a regular function.
(s/fdef create-new-todo>>
  :args (s/cat :data (s/keys :req [::local-store* ::app-db* :todo/title]))
  :ret p/promise?)

;; Finally we wrap it so that it will receive the stores in its
;; parameters (and any other thing defined in the middlewares).
(def <create-new-todo>>
  (m/wrap set-route>>))
```

One last mandatory setup is to implement a `Watchable` protocol for all
references so that Mook can fire re-renders on state transitions. It is already
implemented for Clojure atoms but not for Datascript databases since it is not a
mandatory dependency.

```cljs
(require '[mook.core :as m])
(require 'datascript.db')

(extend-type datascript.db/DB
  m/Watchable
  (m/listen! [this key f]
    (d/listen! this key (fn watch-changes [{:keys [db-after] :as _transaction-data}]
                          (f {::m/new-state db-after}))))
  (m/unlisten! [this key]
    (d/unlisten! this key)))
```

Take a look at:
- The [todomvc-direct-ref-mutations](/examples/todomvc-direct-ref-mutations) example.
- Its [store](/examples/todomvc-direct-ref-mutations/src/todomvc/stores.cljs) definition.
- Its definitions of the [commands](/examples/todomvc-direct-ref-mutations/src/todomvc/commands.cljs).

### State store middleware, version 2: declarative mutations

If we want to use the declarative approach, we have... nothing to do.

The command will receive the same keys but we can only use the state values
(that are immutable values).

The state store middleware merges all the states and stores in the data provided
to a command. In our case, for the input: `{:foo "bar"}`, the command will
receive the following map:

```cljs
{:foo "bar"
 ::local-store {...}
 ::local-store* <Atom ...> ;; <- Present but useless
 ::app-db #datascript/DB{...}
 ::app-db* <DB connection ...> ;; <- Present but useless
 }
```

This would be a command definition:

```cljs
(require '[mook.core :as m])
(require '[datascript.core :as d])
(require '[promesa.core :as p])

;; The command
(defn create-new-todo>> [{::keys [app-db local-store] :as data}]
  (let [title (:todo/title data)
        new-app-db (d/db-with app-db
                              [{:todo/title title
                                :todo/completed? false
                                :todo/created-at (js/Date.)}])
        new-local-store (assoc local-store
                               ::latest-todo
                               title)]
    (p/resolved
      (-> data
          (dissoc :todo/title)
          (assoc ::m/state-transitions [{::m/state-key ::app-db
                                         ::m/new-state new-app-db}
                                        {::m/state-key ::local-store
                                         ::m/new-state new-local-store}])))))

;; We can spec it! It is a regular function.
(s/fdef create-new-todo>>
  :args (s/cat :data (s/keys :req [::local-store ::app-db :todo/title]))
  :ret p/promise?)

;; Finally we wrap it so that it will receive the stores in its
;; parameters (and any other thing defined in the middlewares).
(def <create-new-todo>>
  (m/wrap create-new-todo>>))
```

Take a look at:
- The [todomvc-declarative-mutations](/examples/todomvc-declarative-mutations) example.
- Its [store](/examples/todomvc-declarative-mutations/src/todomvc/stores.cljs) definition.
- Its definitions of the [commands](/examples/todomvc-declarative-mutations/src/todomvc/commands.cljs).

### Commmands: subtle differences

By taking a close look at two versions of the same command
(`create-new-todo>>`), we can see that:
- Direct ref version: destructured keys and spec use the "star" version of the
  state stores: `::local-store*` and `::app-db*`. This convention indicates the
  use of the reference itself (aka **the store**).
- Declarative version: destructured keys and spec use the "starless" version of
  the state store: `::local-store` and `::app-db`. This convention indicates the
  dereferenced version of the state stores, and thus immutable values (aka **the
  state**)

### Mook commands advantage

An interesting thing with this approach is that local commands and global
commands can be coordinated easily. There is an example of this in the
[introductory article](https://lambdam.com/blog/2020-09-mook/#mook-book-app) (in
the `onClick` handler of the `book-detail` component). There is another one in
the [TodoMVC
examples](/examples/todomvc-declarative-mutations/src/todomvc/commands.cljs#L40).

Also, I declared [promesa](https://github.com/funcool/promesa) as a Mook
dependency. This is intentional since it exposes a very nice API to work with
async logic. **In other words, async logic of Mook commands should be structured
with promesa.**  
Check this part of the [TodoMVC
example](https://github.com/lambdam/mook/blob/14ef9df029ddb8a72ff8b5fed5c0a318c9360fac/examples/todomvc-mook-wrapper/src/todomvc/components.cljs#L42).


## Why "Mook"

The main tools used in Mook are Promises and Hooks.

We could craft names such as "Pook" or "Prooks"... but that doesn't sound very
good. And since promises and monads are conceptually very close, we can say that
the library is about MOnads and hoOKs: "Mook".


## Examples

I applied Mook to the TodoMVC project and included the sources of the examples
in the repository, in the `examples` folder:

- TodoMVC with [Mook own React wrapper and the commands with direct ref mutations](/examples/todomvc-direct-ref-mutations).
- TodoMVC with [Mook own React wrapper and the commands with declarative mutations](/examples/todomvc-declarative-mutations).
- TodoMVC with the [hicada library](/examples/todomvc-hicada).  
  [Hicada](https://github.com/rauhs/hicada) is a hiccup compiler for ClojureScript.
- [COMING SOON] TodoMVC with the helix library.


## Todo

* [ ] Unit tests
* [ ] Server side rendering for the small wrapper


## License

Copyright © 2020 Damien RAGOUCY

Distributed under the [MIT License](/license.txt)
