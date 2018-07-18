## Plugins

Plugins are used to extend ventas in a similar way to entity types.

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
     :db/cardinality :db.cardinality/one}]})
```

The idea is that independent authors should be able to distribute functionality that users can include in their stores.

The plugin above could be uploaded to Clojars and then `require`d in the store project.

FAQ:

- Can I install or uninstall a plugin from the backoffice?
  No you can't. That would require, as a minimum, a restart of the jvm process, but running unknown code in a production store is a bad idea anyway.
- Can I upgrade a plugin from the backoffice?
  That would also be running unknown code in production, so no. Ventas may add some notification system telling you that a new version of a plugin is available, but that's it.
- Can I disable a plugin from the backoffice?
  See the first question.

You can use the [lein template](https://github.com/JoelSanchez/ventas-lein-template) to begin with plugin development.