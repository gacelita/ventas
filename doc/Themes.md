## Themes

Themes are used for choosing between different frontends for your store, both in production and in development.

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

The most basic theme might be this one:

```clojure
(ns my.awesome.theme.core
  (:require
    [ventas.core] ;; very important too, sets up websockets, routes...
    [ventas.routes :as routes]))

(defn page []
  [:p "ventas is awesome!"])

(routes/define-route!
  :frontend
  {:name "My awesome theme's home"
   :url ""
   :component page})
```

For your theme to work, you need to have:

-  A minified CLJS build with your theme's main namespace as `:main`, and an output-to of `resources/public/files/js/compiled/{{theme}}.js`
-  A `resources/public/files/css/themes/{{theme}}.css` file.

You can set these up like this, in project.clj

```clojure
{:sassc {:src "src/scss/whatever/you/want.scss"
         :output-to "resources/public/files/css/themes/awesome.css" ;; important
         :style "nested"
         :import-path "src/scss"}}
```

And:

```clojure
{:cljsbuild
 {:builds [{:id "my-theme-build" ;; will be used in prep-tasks, see next section
            :compiler {:main 'my.awesome.theme.core ;; must be equal to :cljs-ns
                       ;; must be like this
                       :output-to (str "resources/public/files/js/compiled/awesome.js")
                       ;; same
                       :output-dir (str "resources/public/files/js/compiled/awesome")
                       :optimizations :advanced}}]}}
```

Finally, ensure that your minified build runs in production:

```clojure
{:profiles
 {:uberjar
  {:prep-tasks ["compile" ["cljsbuild"
                           "once"
                           "my-theme-build" ;; your build ID
                           ]]}}}
```

You can use the [lein template](https://github.com/JoelSanchez/ventas-lein-template) to begin with theme development.
