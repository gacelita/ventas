# ventas

ventas wants to be an eCommerce platform written entirely in Clojure.

For now, the project is a WIP. Although the foundation is good, there aren't many features yet (and no tests whatsoever).

For any questions I'm always available at my email (which you can find in `project.clj`). You can also open an issue.

### Overview

#### Backend

- Written in Clojure.

- Uses [mount](https://github.com/tolitius/mount) and really likes REPL-driven development. Code reload is done by calling `repl/r `. App initialization is done by calling `repl/init`.

- The database is Datomic. A custom database entity system, which relies on core.spec, abstracts the database and allows easy testing and generation of sample data.

  ```clojure
  (seed/seed :recreate? true) ;; recreates the database and seeds it with random entities
  ```
  Lots of utility functions make exploring the database and getting data from it more interactive and fast.

  ```clojure
   ;; returns a list of active users
  (entity/query :user {:status :user.status/active})
  ;; returns an entity by EID
  (entity/find 17592186045760)
  ;; creates an user and returns the result
  (entity/create :user {:email "test@email"
                        :first-name "Test"
                        :last-name "User"})
  ;; generates one user
  (seed/generate-1 :user)
  ;; updates this user's company
  (entity/update {:id 17592186045920
                  :company "Test"})
  ```



  Adding new entities is easy and schema additions are handled behind the curtains (search for calls to `entity/register-type!` to know more)


- The HTTP server is http-kit. Routing is handled by [Compojure](https://github.com/weavejester/compojure), but they are just 4 handlers, because the actual communication happens over websockets, with the help of [chord](https://github.com/jarohen/chord). 

  ```clojure
  (register-endpoint!
    :products/get
    (fn [{:keys [params]} state]
      (entity/to-json (entity/find (:id params)))))
  ```

- Authentication is done with JWT tokens (provided by [buddy](https://github.com/funcool/buddy)).

#### Frontend

- Written in ClojureScript, and uses re-frame.

- Development is done with figwheel, whose state is handled by the server (so you can restart it from the server's REPL).

- Communication with the server is done using a re-frame effect that abstracts websocket requests. All requests and responses are logged to the `verbose` level of the JS console, so you can see what's going on. 

- Client-side routing is handled by [bidi](https://github.com/juxt/bidi), but a custom wrapper exists for it, which makes things much easier to deal with.

  ```clojure
  (routes/define-route!
   :frontend.product ;; this route is nested inside the :frontend route
   {:name (i18n ::the-name-of-this-page)
    :url ["product/" :id]
    :component a-re-frame-component-for-this-route})
  ```

  ​

- Styles are written in SCSS. The watcher is also handled by the server's REPL.

- i18n is done with [tongue](https://github.com/tonsky/tongue)




### Contributing

Prior to writing any code, discuss with me what you want to do via issue or email (you'll find my email in `project.clj`, as I mentioned). Doing otherwise might result in wasted effort, which is never good.

If you wish to contribute but you don't know what do to, read this: [TODO](./TODO.md)