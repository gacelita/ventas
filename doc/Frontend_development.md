## Frontend development

I'll assume you have created an endpoint and want to use it from the frontend and show what it returns in a new route.

First, you create an event handler in `ventas.events.backend`, using the endpoint's name:

```clojure
(rf/reg-event-fx
 ::my-data.get
 (fn [_ [_ options]]
   {:ws-request (merge {:name :my-data.get} options)}))
```

This is done to add a level of indirection over the endpoint name. If the endpoint's name changes, the fix is simply this way. A more detailed explanation of `:ws-request` is covered in the Effects section.

After that, you define a route, a component for it, and an event handler which will be called when the user navigates to the route:

```clojure
(defn page [])

(rf/reg-event-fx
  ::init
  (fn []))

(routes/define-route!
  :frontend.show-my-data ;; read in the Routes section the rationale for this name
  {:name "Page for showing my-data"
   :url "show-my-data"
   :component page
   :init-fx [::init]})
```

In the event handler, you should fetch whatever your route's initial load needs. In this case, it's just a call to `::backend/my-data.get`:

```clojure
(rf/reg-event-fx 
  ::init
  (fn [_ _]
    {:dispatch [::backend/my-data.get
                {:success [::events/db ::state]}]}))
```

This will make the websocket request and save the result in `::state`, inside `db`.

After that, subscribe to that db path inside your component:

```clojure
(defn page []
  (let [my-data @(rf/subscribe [::events/db ::state])]
    [:pre "This is my data!" (with-out-str (cljs.pprint/pprint my-data))]))
```

You'll notice that, when done this way, what gets rendered is *just* what you gave, with no header, footer, etc. This is by design: routes have control over everything that's on the screen. To include those shared UI elements, you use a *skeleton*:

```clojure
(defn page []
  [ventas.themes.clothing.components.skeleton/skeleton
   (let [my-data @(rf/subscribe [::events/db ::state])]
    [:pre "This is my data!" (with-out-str (cljs.pprint/pprint my-data))])])
```



### Routes

#### Defining routes

The `define-route!` shown above takes two arguments: the route keyword and the route options.

It's very important to define the routes starting with `:frontend`, because `define-route!` creates a nested structure, and without the `:frontend` prefix, your route would not be considered a child of the root route (`/`). For example, admin pages begin with`:admin` because they don't shouldn't be considered regular frontend routes.

So, the route data contains:

- `:name` - Can be a function, a keyword, a string, or a vector.
  -  A keyword means it's an i18n identifier and will be treated as such.
  - A vector means it's a subscription. Returning a subscription is useful when your route's name will change over time, for example if it depends on server data (for example, including the name of a product when passing the product page's name). Example: `[::events/db :my-route-name]`
  - When passed a function, it will be called and the result will be considered the route's name.
  - Of course, a string means that's the name :)
- `:url` - The url for this route, relative to its parent. If you had a `:frontend.checkout.something` route, with a `something` URL, the real URL would be `/checkout/something`. The URL can also be a vector, to add bidi parameters: `[:id "/edit"]`
- `:component` - The component that will render the page. Not much to explain.
- `:init-fx` - Will be called when the route is loaded, or its parameters have changed. Good place to fetch data.

#### Resolving routes

Use `routes/path-for`:

```clojure
(routes/path-for :frontend.product :id 5) ;; /product/5
```

#### Getting current route info

```clojure
;; current route keyword
(routes/handler) ;; :frontend.product
;; current route params
(routes/params) ;; {:id 5}
;; both keyword and params
(routes/current) ;; [:frontend.product {:id 5}]
;; route name
(routes/route-name) ;; "Product Five"
```

#### Going to a route

Use `routes/go-to`.

```clojure
(routes/go-to :frontend.product :id 5)
```

You can also use the effect handler:

```clojure
(rf/reg-event-fx
  ::whatever
  (fn [_ _]
    {:go-to [:frontend.product :id 5]}))

```

### ws-request

This is the only effect handler used to communicate with the server. It takes these parameters:

- `:name` - The endpoint's name
- `:params` - Parameters to be sent to the endpoint.
- `:success` and `:error` - can take many things :)
  - A function, which will be called with the response.
  - A vector representing a dispatch value. The response will be added as the last parameter. So, if you have `[::events/db ::something]`, the dispatch that will be performed is `[::events/db ::somethig response-data]`
  - A keyword representing an event handler. Same as the vector version.

### ::events/db

This is called the "universal subscription" and the "universal effect handler".

Let's talk about the subscription first.

re-frame developers advocate for declaring every subscription separately, even if they just extract a path from the database:

```clojure
(rf/reg-sub
 ::something
 (fn [db _] (-> db :something)))
```

I think that's a waste of energy and keystrokes, that's why I use `::events/db`. It takes just one parameter:

```clojure
;; (-> db :my-key)
@(rf/subscribe [::events/db :my-key])
;; (-> db :my :nested :key)
@(rf/subscribe [::events/db [:my :nested :key]])
```

Regarding the universal effect handler, it does what you should be expecting by now:

```clojure
;; (assoc db :my-key "Data!")
(rf/dispatch [::events/db :my-key "Data!"])
;; (assoc-in db [:my :nested :key] "Data!)
(rf/dispatch [::events/db [:my :nested :key] "Data!"])
```

