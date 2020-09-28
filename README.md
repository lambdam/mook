# Mook

[![Clojars Project](https://img.shields.io/clojars/v/mook.svg)](https://clojars.org/mook)

Mook is a library designed to handle frontend application state(s). It serves
the same purpose than re-frame.

⚠️**This library is in an experimental state. Depending on the feedbacks that I
would receive in the coming weeks, things can change.**  
But that's the point: play with the code and send feebacks (Clojurians Slack
"mook" channel or pull requests)! Check the [TodoMVC exemple](/example).

Also, check the [interactive article](https://lambdam.com/blog/2020-09-mook/)
that introduces the library and explains the design decisions.

```clj
;; Clojure CLI/deps.edn
mook {:mvn/version "0.1.0-SNAPSHOT"}

;; Leiningen/Boot
[mook "0.1.0-SNAPSHOT"]
```

## Design ideas

- Query the state directly and locally in components.
- Use directly Clojure references (generally atoms) in state transitions (aka
  actions, or commands in Mook parlance).
- Use promises to handle async workflows.
- Make actions/commands composable.
- Enable the use of Datascript along with atoms to store state.

## Usage

Mook introduces the notion of state stores: instead of limiting the application
state to one big hashmap, mook enables to have multiple stores.  
Typically two types of state stores can be used:
- A Datascript database that would hold state that flows troughout all the
  architecture (frontend and backend)
- A Clojure atom that would hold frontend only state and that acts like a
  lightweight key-value store.
  
### Setup

To unify the watching mechanism of atoms and Datascript dbs, mook uses a
protocol that is implemented for atoms but not for Datascript since it is not a
mandatory dependency.

```cljs
(ns my.app)

(require '[mook.core :as m])
(require '[datascript.core :as d])
(require 'datascript.db)

(extend-type datascript.db/DB
  m/Watchable
  (m/listen! [this key f]
    (d/listen! this key (fn watch-changes [{:keys [db-after] :as _transaction-data}]
                          (f {::m/new-state db-after}))))
  (m/unlisten! [this key]
    (d/unlisten! this key)))
```

Then we have to register state stores in mook.

```cljs
;; Datascript

(def db-schema {...})

(defonce db* (d/create-conn db-schema))

(m/register-store! ::db* db*)

;; Atom (lightweight store)

(defonce local-store*
  {::current-user-id nil
   ::in-progress? false
   ...})

(m/register-store! ::local-store* local-store*)
```

Next, we have to wrap the root component with the mook context.

```cljs
(defn root-component [props]
  ...)

(js/ReactDOM.render
  (m/mook-state-store-container
    (js/React.createElement root-component nil))
  (js/document.getElementById "mook-block-1"))
```

Mook is only about state management. But Mook relies on the Hooks API (React >=
16.8).  
For the view you can use:
- The bare React library
- The very thin wrapper included in Mook. Check the
  [documentation](/doc/react-wrapper.md).
- Another hook ready wrapper like [helix](https://github.com/lilactown/helix) or
  [crinkle](https://github.com/favila/crinkle) (not tested yet).

For the following examples, we will use the thin wrapper included in Mook.

Now, there are two things that we might do with our application: read data from
the state stores and modify the state stores.

### Read the state(s)

Mook defines two hooks: `use-mook-state` and `use-param-mook-state`.

Mook hooks have two arities: the unary one that accepts a map with all
parameters explicitly given. In a way, this arity acts like labelled arguments
in other languages (like OCaml for example). Respectively the binary and ternary
arities with positional arguments act like shorthand versions of the function
call.

`use-mook-state` takes a state store name and a handler. The handler receives
the dereferenced store as its first and only parameter. There are only two ways
for this hook to fire a re-render:
1. the result of the handler changes (the handler might close over changing
values)
2. the state store changes and the result of the previous known handler changes.

```cljs
(require '[mook.core :as m])

;; Arity 1
(use-mook-state {::m/store-key ::local-store*
                  ::m/handler (fn [store]
                                (::current-user-id store))})

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
(use-param-mook-state {::m/store-key ::db*
                        ::m/params [current-user-id book-ids]
                        ::m/handler (fn [db] ...)})

;; Arity 3 (shorthand)
(use-param-mook-state ::db*
                       [current-user-id book-ids]
                       (fn [db] ...))
```

### Mook commands (~ actions)

Traditionally, mutating the global state is done through "actions". I chose
another semantics after a discussion with a friend: "commands". The semantics is
taken from the event sourcing architecture that distinguishes "facts", things
that happened for sure, and "commands", sending the intention of a
transformation. But this command can fail for many reasons. A **command** in
mook is a **function that takes a map and returns a promise that resolves to a
map**. The promise expresses the fact that the future result of a command can be
a success or a failure. Also the promise has the useful property to be
chainable.

Global commands have to be registered so that the mook state stores can be
merged into their map argument.

```cljs
(require '[mook.core :as m])

;; The command
(defn set-route [{::keys [local-store*] :as data}]
  (swap! local-store* merge (select-keys data [::current-route]))
  (p/resolved (dissoc data ::current-route)))

;; We can spec it! It is a regular function.
(s/fdef set-route
  :args (s/cat :data (s/keys :req [::local-store* ::current-route]))
  :ret p/promise?)

;; Finally we register it so that it will receive the stores in its parameters
(m/register-command! ::set-route set-route)
```

And then, commands can be called this way:

```cljs
(m/send-command>> {::m/type ::set-route
                   ::current-route :by-category})
```

The only mandatory key for the command data is `mook.core/type`.

In the example above, the map received by the `set-route` function will look
like this:

```cljs
{:mook.core/type :my.app/set-route
 :my.app/current-route :by-category
 :my.app/local-store* <clojure atom ref>
 :my.app/db* <Datascript ref>}
```

An interesting thing with this approach is that local commands and global
commands can be coordinated easily. There is an example of this in the
[introductory article](https://lambdam.com/blog/2020-09-mook/#mook-book-app) (in
the `onClick` handler of the `book-detail` component).

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

- TodoMVC with [Mook own React wrapper](https://github.com/lambdam/mook/tree/master/examples/todomvc-mook-wrapper).
- TodoMVC with the [hicada library](https://github.com/lambdam/mook/tree/master/examples/todomvc-hicada).  
  [Hicada](https://github.com/rauhs/hicada) is a hiccup compiler for ClojureScript.
- [COMING SOON] TodoMVC with the helix library.

## Todo

* [ ] Unit tests
* [ ] Server side rendering for the small wrapper

## License


Copyright © 2020 Damien RAGOUCY

Distributed under the [MIT License](/license.txt)
