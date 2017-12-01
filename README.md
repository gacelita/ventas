# ventas

ventas wants to be an ecommerce platform written entirely in Clojure.

For now, the project is a WIP. Although the foundation is good, there aren't many features yet (and no tests whatsoever).

For any questions I'm always available at my email (which you can find in `project.clj`). You can also open an issue.



### Motivation

No open source ecommerce project satisfies me. I've been working with most of them for years and I think they suck. I won't name any, but this is roughly what I think about the available solutions:

- They tend to be **difficult to extend or modify**. They try to tackle the problem with extension systems, but in the end you need to modify the code of the core to do meaningful changes. This forces you to choose between never updating the software or making an exceptional effort to keep your changes applied. This is why one of the main design decisions for this project is to make it very easy to extend and modify.

- They tend to be **difficult to reason about**. Because they are built upon a fundamentally mutable model, it's impossible to know how did the database get to the current state. In the best case, something bad happens and I don't know why. In the worse case, something bad happens and I don't even notice (until it's too late).

  In the application side of things, mutable objects everywhere doesn't exactly help either. Once I was doing a rename of the ten thousand products of a store. The new names came from a CSV file, and the code was something like this:

  ```php
  $product = new Product($id);
  $product->name = $newName;
  $product->save();
  ```

  I was running the migration on a few test products first, just in case something went wrong with it. And every time I ran the migration, *the price of some of the products was getting smaller* by a small percent. Further debugging revealed that the products that had a discount were getting their discount applied to the base price every time I ran the code, because the `price` property was actually the base price with the discounts applied, but when saving the products I was making that price the base price (without knowing). So the fix was:

  ```php
  $product = new Product($id);
  $product->name = $newName;
  $product->price = $product->basePrice;
  $product->save();
  ```

  Needless to say, it's ridiculous to expect me to know that. I filed a bug and got no response.

- They tend to have **poor performance out of the box**. Of course everything can be made performant, but I shouldn't need to make the effort. Particularly when "effort" means rewriting SQL queries, or wasting several days trying to find out what's causing my store to take 20 seconds to load.

- They tend to be **over-engineered**, or having user-hostile "features". This is a problem in a lot of software, but it's there nonetheless.



### Getting started

At the moment, ventas is unfit for its purpose. However, if you are a developer and just want to see the project in action, read on.

You need to have  `git` and `leiningen` installed. You also need access to a Datomic database.

First `clone` the project and `cd` into it:

```bash
git clone https://github.com/JoelSanchez/ventas
cd ventas
```

Then, go to `resources` and copy the example configuration file:

```
cp resources/config.example.edn resources/config.edn
```

Now you can start the REPL:

```
lein repl
```

When the REPL is ready, execute `init`:

```clojure
repl=> (init)
:reloading (ventas.common.utils ventas.utils ventas.config ventas.database ventas.database.schema ventas.database.entity ventas.entities.product-variation ventas.database.generators ventas.entities.i18n ventas.entities.brand ventas.plugin ventas.database.seed ventas.entity-test ventas.events repl ventas.entities.image-size ventas.paths ventas.entities.file ventas.server.ws ventas.server ventas.server-test ventas.auth ventas.entities.user ventas.test-tools ventas.database-test ventas.entities.product-taxonomy ventas.server.pagination ventas.utils.images ventas.server.api ventas.entities.configuration ventas.entities.address ventas.entities.product-term client ventas.plugins.featured-categories.core ventas.plugins.slider.core ventas.entities.order-line ventas.entities.order ventas.common.utils-test ventas.entities.resource ventas.entities.category ventas.entities.product ventas.entities.country ventas.entities.tax ventas.entities.state ventas.plugins.blog.core ventas.plugins.featured-products.core user)
Starting database, URL: datomic:dev://localhost:4334/ventas
Starting server
INFO [ventas.server:118] - Starting server on 0.0.0.0:3450
Starting Figwheel
Figwheel: Starting server at http://0.0.0.0:3449
Figwheel: Watching build - app
Figwheel: Cleaning build - app
Compiling "resources/public/files/js/compiled/ventas.js" from ["src/cljs" "src/cljc" "test/cljs" "test/cljc" "custom-lib"]...
Successfully compiled "resources/public/files/js/compiled/ventas.js" in 19.013 seconds.
Figwheel: Starting CSS Watcher for paths  ["resources/public/files/css"]
Starting SASS
:done
```

Now you can open `localhost:3450` to see the frontend! It's recommended that you enable the `verbose` logging level in the console, to see what's going on.

The backoffice's URL is `localhost:3450/admin`, which currently requires no auth :)

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