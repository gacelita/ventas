### How to add an endpoint

Call `ventas.server.api/register-endpoint!`.

```clojure
(ventas.server.api/register-endpoint!
  :capitalize
  {:spec {:string string?}
   :doc "Returns given string, but capitalized"}
  (fn [{{:keys [string]} :params}]
    (clojure.string/capitalize string)))
```

The first argument identifies the endpoint, and is used by the clients to make the request.

The second argument is optional, and may contain `:spec`  and `:doc` keys. The `:spec` is a [data spec](https://github.com/metosin/spec-tools#data-specs), to be applied to the incoming `:params`.

The third argument is the request handler, which will be called with two arguments:

- Ventas' request data. A map consisting of:
  - `:name` - the name of this endpoint (`:capitalize` in the example above).
  - `:params` - whatever params were passed. Validated against the provided `:spec`.
  - `:type` - will just be `:request` for now. Could also be `:event` but it's not currently in use.
  - `:id` - the request identifier, which the client needs to know what to do when the response arrives (which client-made request the response corresponds to).
- State data. A map consisting of:
  - `:request` - Ring's request, created when connecting the websocket.
  - `:client-id` - An UUID that identifies this websocket client.
  - `:channel` - core.async channel corresponding to this websocket connection.
  - `:session` - An atom which contains session data for this websocket connection.

Most of the time, you'll only care about `:params` and `:session`.

Another example, using `:session`:

```clojure
(ventas.server.api/register-endpoint!
  :greet
  {:doc "Greets the user"}
  (fn [_ {:keys [session]}]
    (let [user-id (:user @session)
          user (ventas.database.entity/find user-id)]
      (str "Hello, " (:user/first-name user) "!"))))
```

The example above could've been written using `get-user`, like this:

```clojure
(ventas.server.api/register-endpoint!
  :greet
  {:doc "Greets the user"}
  (fn [_ {:keys [session]}]
    (let [user (ventas.server.api/get-user session)]
      (str "Hello, " (:user/first-name user) "!"))))
```

If you throw an exception, the request will return `:success false` and your exception's message inside `:data`. So this request:

```clojure
(ventas.server.api/register-endpoint!
  :bad-request
  (fn [_ _]
    (throw (Exception. "This request is bad!"))))
```

...would result in this:

```clojure
{:type :response
 :id "request-users.session-1"
 :success false
 :data "This request is bad!"}

```



### Trying endpoints

Call `ventas.server.ws/call-request-handler`:

```clojure
(ventas.server.ws/call-request-handler
 {:name :my-request
  :params {:some :params}})
```

You can also trick ventas into thinking you're a certain user:

```clojure
(ventas.server.ws/call-request-handler
  {:name :greet}
  {:session (atom {:user (:db/id (ventas.database.entity/query-one :user))})})
```

This would result in:

```clojure
{:type :response
 :id nil
 :success true
 :data "Hello, Test!"}
```

This operation is quite common, so there's a function for it:

```clojure
(ventas.server.ws/call-handler-with-user
  :greet
  nil
  (ventas.database.entity/query-one :user))
```

You can also use the Swagger-like API tool, which is usually under `/api `.