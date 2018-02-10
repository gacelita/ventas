## Sites

Sites are used to use a different ElasticSearch index and Datomic database depending on the host that makes the request.

To add a site, add a file in this way: `storage/sites/SITE_NAME.edn`. That file must contain a subset of the `default-config.edn` file. Currently only `:database :url` and `:elasticsearch :index` are supported.

```clojure
{:database {:url "datomic:dev://localhost:4334/otherdatabase"}
 :elasticsearch {:index "otherindex"}}
```

Requests coming from `http://SITE_NAME` will use the specified database and index.

In the REPL, you can use a certain site by using `with-site`:

```clojure
(ventas.site/with-site
 "SITE_NAME"
 #(ventas.database.seed/seed))
```

