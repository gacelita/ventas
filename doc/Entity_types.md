## Entity types

Entity types are like "database classes". This is an entity with an `:user` entity type:

```clojure
{:schema/type :schema.type/user
 :user/email "john@test.com
 :user/roles #{:user.role/administrator}}
```

The `:schema/type` attribute identifies this entity as an `:user`, and thanks to that, ventas will know how to denormalize it and how to validate its fields (using spec).

Here's an example `:user` entity type:

```clojure
(entity/register-type!
 :user
 {:attributes
  (concat
   [{:db/ident :user/email
     :db/valueType :db.type/string
     :db/index true
     :db/unique :db.unique/identity
     :db/cardinality :db.cardinality/one}
    
    {:db/ident :user/password
     :db/valueType :db.type/string
     :db/index true
     :db/unique :db.unique/identity
     :db/cardinality :db.cardinality/one}

    {:db/ident :user/roles
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/many
     :ventas/refEntityType :enum}
    
    {:db/ident :user.role/administrator}])

  :to-json
  (fn [this params]
    (-> ((entity/default-attr :to-json) this params)
        (dissoc :password)))

  :filter-create
  (fn [this]
    (utils/transform
     this
     [(fn [user]
        (if (:user/password user)
          (update user :user/password hashers/derive)
          user))]))})
```

Step by step:

- We call `entity/register-type!` passing the identifier for the entity type (`:user`) and a map of properties.
- The first property we add is `:attributes`. Those are just plain old Datomic attributes, but there's a catch: their idents should have a namespace that matches the entity type identifier. Else you'll have to implement more complicated normalization and denormalization functions.
- The next property is `:to-json`, which has a misleading name because it's meant for denormalization (i.e. preparing it to be sent to the clients). In this example, we `dissoc` the password because it would be a security issue to send it to the outside (even if it's encrypted). The `entity/default-attr` call just gets the standard denormalization function, which does most of the job for us.
- The last property is `:filter-create`. A "filter" is a function that should transform whatever is passed to it, and be side-effect free. In this case, we encrypt the user's password.

Entity types are the primary way of extending the database, so you should be familiar with them.

By the way, you should also add a spec for your entity type: it's the primary way of validating data (when updating or creating entities with your entity type):

```clojure
(spec/def :user/roles
  (spec/coll-of #{:user.role/administrator} :kind set?))

(spec/def :user/email
  (spec/and string? #(str/includes? % "@")))

(spec/def :user/password string?)

(spec/def :schema.type/user
  (spec/keys :req [:user/roles
                   :user/password
                   :user/email]))
```



### The :ventas.database/entity namespace

This namespace not only allows you to register entity types: it's what makes them so useful too.

A few examples:

- `(entity/find eid)`: returns the entity with the given eid.
  There's also `(entity/find-json)` which calls `find` and then `to-json`.

- `(entity/to-json an-entity)` will denormalize an entity to be sent to clients. It removes the namespace from the keys (so `:user/name` would be `:name`), it removes the entity type, and it resolves whatever entities there are inside it (as long as they are declared as `:autoresolve? true`, more on that later). So if the user has a `:user/favorites` field containing product IDs, they would be resolved and denormalized.

- `(entity/query)`: allows you to explore the database, looking for entities with a certain entity type. For example, `(entity/query :user)` would get all users, and `(entity/query :user {:name "John"})` would get all users named "John".
  There's also `(entity/query-one)` which does what you'd expect.

- `(entity/create*)` and `(entity/create)`: here, the asterisk means it's a lower level function, which means you'll have to add the namespace to the attributes, and the entity type, by yourself.
  With asterisk:

  ```clojure
  (entity/create* {:schema/type :schema.type/user
                   :user/name "John"
                   :user/roles #{:user.role/administrator}})
  ```

  Without asterisk:

  ```clojure
  (entity/create :user {:name "John"
                        :roles #{:user.role/administrator}})
  ```

  ​

- `(entity/update*)` and `(entity/update)`: same concept as `create`:

  ```clojure
  (entity/update* {:db/id 111172983
                   :user/name "Daniel"})
  (entity/update {:id 111172983
                  :name "Daniel"})
  ```

  However, there's a catch: with `:db.cardinality/many` attributes, you might want to set the value, or append to it:

  ```clojure
   ;; just add 118761786 as a favorite
  (entity/update* {:db/id 111172983
                   :user/favorites 118761786}
                  :append? true)
  ;; set these products as the only favorites
  (entity/update* {:db/id 111172983
                   :user/favorites [118761786 118761787 118761788]})
  ```

- `(entity/upsert*)` and `(entity/upsert)`: they simply call `create(*)` or `update(*)` depending on the existence of the `:db/id` or `:id` attribute.

- `(entity/delete)`: removes an entity by ID.



### Full register-type! API

- `:fixtures` - should be a function that returns a collection of entities that should be present in the database. Including `:schema/type` is not required.

  ```clojure
  {:fixtures (fn []
               [{:user/name "John"}
                {:user/name "Daniel"}])}
  ```

- `:autoresolve?` - whether `to-json` should resolve references to entities with this entity type.

- `:component?` - whether this entity type is meant to be used as a Datomic component. Important for search indexing: component entity types will not be indexed.

- `:attributes` - a collection of Datomic attributes. Used for migrations.

- `:seed-number` - when calling `ventas.database.seed/seed` with the `generate?` option, this property controls how many entities should be generated for this entity type.

- `:dependencies` - if the entity type depends on another entity type (i.e. it has a `:db.type/ref` field that is meant to refer to another entity type), it should be declared here, to seed the entity types in the correct order.
  Example: `{:dependencies #{:i18n :product}}`

- Lifecycle functions:

  - `:filter-seed` - filters entities for seeding (`ventas.database.seed/seed`)
    `:filter-create` - filters entities before creating them (`entity/create(*)`)
    `:filter-update` - filters entities before updating them (`entity/update(*)`)
    `:before-seed` - side-effectful function to run before seeding
    `:before-create` - side-effectful function to run before creating
    `:before-delete` - side-effectful function to run before deleting
    `:after-seed` - side-effectful function to run after seeding
    `:after-create` - side-effectful function to run after creating
    `:after-delete` - side-effectful function to run after deleting



TODO: Why don't`:after-update` and `:before-update `exist?!