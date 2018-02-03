## Plugins

**Warning**: this is a draft, don't actually use these instructions for now.

Plugins are used to allow third parties to add frontend components, endpoints or entity types, in an isolated and controlled way.

To register a plugin:

```clojure
(ventas.plugin/register!
  :awesome
  {:name "A plugin for adding _awesome_ to your store"
   :fixtures
   (fn []
     [{:category/name (ventas.entities.i18n/get-i18n-entity
                        {:en_US "Awesome products"})}])
   :migrations
   [{:db/ident :awesome/entity
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}]})
```

