## Themes

Themes are used for choosing between different frontends for your store, both in production and in development.

Themes can be registered using the `ventas.theme/register!`  fn:

```clojure
(ventas.theme/register!
  :awesome
  {:name "An awesome theme"
   :build {:main 'my.awesome.theme.core}
   :migrations
   [{:db/ident :product.term/color
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}]
   :fixtures
   (fn []
     [{:schema/type :schema.type/image-size
       :image-size/width 120
       :image-size/height 180
       :image-size/algorithm :image-size.algorithm/crop-and-resize
       :image-size/entities #{:schema.type/product}}])})
```

The most basic theme might be this one:

```clojure
(ns my.awesome.theme.core
  (:require
    [ventas.core] ;; very important, sets up websockets, routes...
    [ventas.routes :as routes]))

(defn page []
  [:p "ventas is awesome!"])

(routes/define-route!
  :frontend
  {:name "My awesome theme's home"
   :url ""
   :component page})
```

That's it, your theme is done. You can set it as the current theme like this:

```clojure
(ventas.entities.configuration/set! :theme :mytheme)
```

If you have many [sites](./Sites.md), you can set the theme for a given site like this (ID 2 in this example):

```clojure
(ventas.entities.configuration/set! :theme :mytheme :site 2)
```

If you change the theme, you need to restart figwheel if you want to use it:

```clojure
(ventas.system/r :figwheel)
```

You can use the [lein template](https://github.com/JoelSanchez/ventas-lein-template) to begin with theme development.



For your theme to be compiled by `lein uberjar`, it needs to be included in the project.clj like this:

```clojure
:themes [:awesome]
```

