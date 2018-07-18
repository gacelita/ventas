## Sites

Sites are used to simulate a multi-tenant environment while using the same Datomic and Elasticsearch database. 

You can add a site like this:

```clojure
;; create an admin user
(def user
  (entity/create :user {:email email
                      :password password
                      :roles #{:user.role/administrator}}))
;; create a site
(entity/create :site {:user (:db/id user)
                      :subdomain "example"})
;; update the site of the user
(entity/update* (assoc user :ventas/site (:db/id site)))
```

A Datomic filter ensures that sites can't access data from other sites via the use of the `:ventas/site`  attribute. The presence of this attribute makes an entity visible only when visiting the subdomain of the site. For example, assume the existence of this site and product:

```clojure
{:db/id 2
 :schema/type :schema.type/site
 :site/subdomain "example"}

{:schema/type :schema.type/product
 :product/name ...
 :product/price ...
 :ventas/site 2}
```

This product would be accessible only by visiting `example.your-domain.com`.

Every site can have its own theme:

```clojure
(ventas.entities.configuration/set! :theme :mytheme :site 2)
```

Also, there's an endpoint called `:site.create` for creating sites by giving an administrator email, a password and the name of the site.