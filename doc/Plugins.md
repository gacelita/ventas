## Plugins

**Warning**: this is a draft, don't actually use these instructions for now (they won't work).

Plugins are used to allow users to conditionally enable optional functionality.

They are not used to add functionality: the things they reference need to exist already.

```clojure
(ventas.plugin/register!
  :awesome
  {
   ;; will be used in the administration
   :name "A plugin for adding _awesome_ to your store"
    
   ;; same semantics as the fixtures of entity types
   :fixtures
   (fn []
     [{:category/name (ventas.entities.i18n/get-i18n-entity
                        {:en_US "Awesome products"})}])
    
   ;; same semantics as the migrations of entity types
   :migrations
   [{:db/ident :awesome/entity
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}]
    
   ;; list of entity types to conditionally enable
   :entity-types [:my-entity-type]
    
   ;; list of endpoints to conditionally enable
   ::endpoints [:my-entity-type.list]})
```

You can use the [lein template](https://github.com/JoelSanchez/ventas-lein-template) to begin with theme development.