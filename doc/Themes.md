## Themes

**Warning**: this is a draft, don't actually use these instructions for now.

Themes are used for choosing between different frontends for your store. They don't need to be third-party provided, although they might be. They don't need to follow any concrete structure.

Themes can be registered using the `ventas.theme/register!`  fn:

```clojure
(ventas.theme/register!
  :awesome
  {:name "An awesome theme"
   :cljs-ns 'my.awesome.theme.core
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

The administrator can choose what theme to use in the administration. When your theme is selected, this is what ventas will do:

- Set your theme's identifier (`:awesome`) as the value for the `current-theme` configuration key, in the database.
- Apply your theme's migrations and fixtures.
- In the clojurescript code, require your theme's main namespace (the one you declared in `:cljs-ns`).

The most basic theme might be this one:

```clojure
(ns my.awesome.theme.core
  (:require
    [ventas.routes :as routes]))

(defn page []
  [:p "ventas is awesome!"])

(routes/define-route!
  :frontend
  {:name "My awesome theme's home"
   :url ""
   :component page})
```

When your theme is disabled, your theme's migrations and fixtures will be undone.

You can use the [lein template](https://github.com/JoelSanchez/ventas-theme-lein-template) to begin with theme development.

@TODO Actually do that template