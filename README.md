<p align="center">
  <img alt="ventas" title="ventas" src="./storage/logo.png" width="100">
</p>

# ventas

[![Build Status](https://travis-ci.org/JoelSanchez/ventas.svg?branch=master)](https://travis-ci.org/JoelSanchez/ventas)
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)

ventas wants to be an ecommerce platform written entirely in Clojure.

For now, the project is a WIP. Although the foundation is good, it's not possible to run a real store with it, as it lacks a checkout process, order management, email templates, and other basic stuff (for now).

For any questions I'm usually available at the `#ventas` channel in [clojurians](clojurians.slack.com). You can also open an issue, or contact me by email (you'll find it in `project.clj`) .

[Documentation](./doc) (but read this document first!)

[Demo instance](https://ventas2.kazer.es) (may not be running)

- [Swagger-like API tool](https://ventas2.kazer.es/api)



### Motivation

No open source ecommerce project satisfies me. I've been working with most of them for years and I think they suck. I won't name any, but this is roughly what I think about the available solutions:

- They tend to be **difficult to extend or modify**. They try to tackle the problem with extension systems, but in the end you need to modify the code of the core to do meaningful changes. This forces you to choose between never updating the software or making an exceptional effort to keep your changes applied. This is why one of the main design decisions for this project is to make it very easy to extend and modify.

- They tend to be **difficult to reason about**. Because they are built upon a fundamentally mutable model, it's impossible to know how did the database get to the current state. In the best case, something bad happens and I don't know why. In the worst case, something bad happens and I don't even notice (until it's too late). Having mutable objects everywhere doesn't exactly help either.

- They tend to have **poor performance out of the box**. Of course everything can be made performant, but I shouldn't need to make the effort. Particularly when "effort" means rewriting SQL queries, or wasting several days trying to find out what's causing my store to take 20 seconds to load.

- They tend to be **over-engineered**, or having user-hostile "features". This is a problem in a lot of software, but it's there nonetheless.





### Getting started

At the moment, ventas is unfit for its purpose. However, if you are a developer and just want to see the project in action, read on.

You need to have  `git`, `sassc` and `leiningen` installed. You also need access to a Datomic database and an Elasticsearch instance. (See [Setting up a local environment with docker-compose](#setting-up-a-local-environment-with-docker-compose) if you feel comfortable with Docker)

First `clone` the project, `cd` into it, and install the frontend dependencies:

```bash
$ git clone https://github.com/JoelSanchez/ventas
$ cd ventas
$ bower install
```

Now you can start the REPL:

```bash
$ lein repl
```

When the REPL is ready, execute `init`:

```clojure
user=> (init)
:reloading (ventas.common.utils ventas.utils ventas.config ventas.database ventas.database.schema ventas.database.entity ventas.entities.product-variation ventas.database.generators ventas.entities.i18n ventas.entities.brand ventas.plugin ventas.database.seed ventas.entity-test ventas.events repl ventas.entities.image-size ventas.paths ventas.entities.file ventas.server.ws ventas.logging ventas.server ventas.server-test ventas.auth ventas.entities.user ventas.test-tools ventas.database-test ventas.entities.product-taxonomy ventas.server.pagination ventas.utils.images ventas.server.api ventas.entities.configuration ventas.entities.address ventas.entities.product-term client ventas.plugins.featured-categories.core ventas.plugins.slider.core ventas.entities.order-line ventas.entities.order ventas.common.utils-test ventas.entities.resource ventas.entities.category ventas.entities.product ventas.entities.country ventas.entities.tax ventas.entities.state ventas.plugins.blog.core ventas.plugins.featured-products.core user)
INFO [ventas.database:27] - Starting database, URL: datomic:dev://localhost:4334/ventas
INFO [ventas.server:99] - Starting server
INFO [ventas.server:102] - Starting server on 0.0.0.0:3450
INFO [client:28] - Starting Figwheel
Figwheel: Starting server at http://0.0.0.0:3449
Figwheel: Watching build - app
Compiling "resources/public/files/js/compiled/ventas.js" from ["src/cljs" "src/cljc" "test/cljs" "test/cljc" "custom-lib"]...
Successfully compiled "resources/public/files/js/compiled/ventas.js" in 8.252 seconds.
Figwheel: Starting CSS Watcher for paths  ["resources/public/files/css"]
INFO [client:42] - Starting SASS
:done
```

Then, create the database, apply the schema and create the fixtures:

```clojure
(seed/seed :recreate? true)
```

Now you can open `localhost:3450` to see the frontend!

You will get HTTP 500 errors when the frontend tries to request the images for the demo products, because they are not included in this repo. To see the images, you should clone [ventas-demo-images](https://github.com/JoelSanchez/ventas-demo-images) inside the `storage` directory.

The backoffice's URL is `localhost:3450/admin`:

```
Username: test@test.com
Password: test
```

Finally, ClojureScript development is usually done by connecting via nREPL and executing `cljs-repl`:

```bash
# the following is an alias for "lein repl :connect localhost:4001"
$ lein nrepl
Connecting to nREPL at localhost:4001
REPL-y 0.3.7, nREPL 0.2.13
Clojure 1.9.0-alpha19
OpenJDK 64-Bit Server VM 1.8.0_144-b01
    Docs: (doc function-name-here)
          (find-doc "part-of-name-here")
  Source: (source function-name-here)
 Javadoc: (javadoc java-object-or-class-here)
    Exit: Control+D or (exit) or (quit)
 Results: Stored in vars *1, *2, *3, an exception in *e

user=> (cljs-repl)
Launching ClojureScript REPL for build: app
Figwheel Controls:
          (stop-autobuild)                ;; stops Figwheel autobuilder
          (start-autobuild [id ...])      ;; starts autobuilder focused on optional ids
          (switch-to-build id ...)        ;; switches autobuilder to different build
          (reset-autobuild)               ;; stops, cleans, and starts autobuilder
          (reload-config)                 ;; reloads build config and resets autobuild
          (build-once [id ...])           ;; builds source one time
          (clean-builds [id ..])          ;; deletes compiled cljs target files
          (print-config [id ...])         ;; prints out build configurations
          (fig-status)                    ;; displays current state of system
          (figwheel.client/set-autoload false)    ;; will turn autoloading off
          (figwheel.client/set-repl-pprint false) ;; will turn pretty printing off
  Switch REPL build focus:
          :cljs/quit                      ;; allows you to switch REPL to another build
    Docs: (doc function-name-here)
    Exit: Control+C or :cljs/quit
 Results: Stored in vars *1, *2, *3, *e holds last exception object
Prompt will show when Figwheel connects to your application
To quit, type: :cljs/quit
```

#### Setting up a local environment with Docker Compose

- `cd datomic`
- `cp .credentials.example .credentials`
- In `.credentials`, replace "username" and "pasword" with your credentials for downloading Datomic from my.datomic.com
- `cd config`
- `cp transactor.example.properties transactor.properties`
- In `transactor.properties`, replace "YOUR_LICENSE_KEY" with your Datomic Pro license key (you can get one for free, see Datomic Starter [here](https://www.datomic.com/get-datomic.html))
- `cd ../../` (repository root)
- `docker build -t datomic datomic`

Now everything is prepared for starting your local environment. Every time you want to use (or develop) `ventas`, you can execute:

`docker-compose up` 

That will start Datomic and Elasticsearch.

### Overview

#### Backend

- Written in Clojure.

- Uses [mount](https://github.com/tolitius/mount) and really likes REPL-driven development. Code reload is done by calling `repl/r `. App initialization is done by calling `repl/init`.

  ```clojure
  ;; (r) reloads changed namespaces, restarts defstates within them, and optionally
  ;; restarts given defstates as keywords
  (r :db)
  INFO [ventas.database:34] - Stopping database
  :reloading ()
  INFO [ventas.database:27] - Starting database, URL: datomic:dev://localhost:4334/ventas
  => :done
  ```

- The database is Datomic. A custom database entity system, which relies on core.spec, abstracts the database and allows easy testing and generation of sample data.

  ```clojure
  ;; recreates the database, applies the schema, creates the fixtures and seeds the database with randomly generated entities
  (seed/seed :recreate? true :generate? true)
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
  ;; generates three users
  (entity/generate :user 3)
  ;; updates an user's company
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

- Styles are written in SCSS. The watcher is also handled by the server's REPL.

- i18n is done with [tongue](https://github.com/tonsky/tongue)

- Heavy usage of [Semantic UI](http://react.semantic-ui.com/) components.


### Resources

[leiningen template](https://github.com/JoelSanchez/ventas-lein-template)

### Contributing

I'd appreciate help in any part of the project.

Please read [CONTRIBUTING.md](./CONTRIBUTING.md)
