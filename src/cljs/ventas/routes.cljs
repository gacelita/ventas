(ns ventas.routes
  "Bidi wrapper and route utilities"
  (:require
   [accountant.core :as accountant]
   [bidi.bidi :as bidi]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.page :as page]
   [ventas.utils :as utils]
   [ventas.utils.logging :as log]))

(defn route-parents
  ":admin.users.something -> [:admin :admin.users :admin.users.something]"
  [route]
  (into [] (map #(keyword (str/join "." %))
                (reduce (fn [acc i]
                          (conj acc (conj (vec (last acc)) i)))
                        []
                        (drop-last (str/split (name route) #"\."))))))

(defn- index-urls
  "Creates a [route -> url] map"
  [routes]
  (reduce (fn [acc {:keys [route url] :as item}]
            (assoc acc route url))
          {}
          routes))

(defn- prepare-routes [routes]
  (let [indexed-urls (index-urls routes)]
    (map (fn [route]
           (let [parent (last (route-parents (:route route)))
                 parent-url (indexed-urls parent)]
             (update route :url #(cond
                                   (= parent-url "") %1
                                   (string? %1) (str "/" %1)
                                   :else (vec (concat ["/"] %))))))
         routes)))

(defn- reducer [acc {:keys [route url] :as item} indexed-urls]
  (let [parents (route-parents route)
        path (concat (map #(% indexed-urls) parents) [url])]
    (update-in acc path #(merge % {"" route}))))

(defn- compile-routes [routes]
  (let [routes (prepare-routes routes)
        indexed-urls (index-urls routes)]
    ["" (-> (reduce (fn [acc item]
                      (reducer acc item indexed-urls))
                    {}
                    routes)
            (assoc true :not-found))]))

(def ^:private route-data
  (atom []))

(def ^:private routes (atom (compile-routes @route-data)))

(defn define-route! [name {:keys [component] :as attrs}]
  (swap! route-data conj (assoc attrs :route name))
  (reset! routes (compile-routes @route-data))
  (when component
    (defmethod page/pages name []
      [component])))

(defn define-routes! [new-routes]
  (swap! route-data concat new-routes)
  (reset! routes (compile-routes @route-data)))

(defn find-route
  "Finds a route by its id"
  [id]
  (first (filter #(= (:route %) id) @route-data)))

(defn path-for
  "bidi/path-for wrapper"
  [& args]
  (let [path (apply bidi/path-for @routes args)]
    (when-not path
      (log/error "Route not found" args))
    path))

(defn match-route
  "bidi/match-route wrapper"
  [& args]
  (apply bidi/match-route @routes args))

(defn go-to [& args]
  (when-let [path (apply path-for args)]
    (accountant/navigate! path)))

(defn current
  "Returns the current route"
  []
  @(rf/subscribe [::events/db :route]))

(defn handler
  "Returns the current route handler"
  []
  (first (current)))

(defn params
  "Returns the current route params"
  []
  (last (current)))

(defn route-name
  "Returns the name of a route"
  [handler & [params]]
  {:pre [(or (nil? params) (map? params))]}
  (let [{:keys [name]} (find-route handler)]
    (cond
      (string? name) name
      (keyword? name) (apply i18n name params)
      (fn? name) (name params)
      (vector? name) @(rf/subscribe name)
      :else (log/warn "A route returns a name that is not a string, keyword, function or vector"
                      handler
                      name))))

(rf/reg-fx
 :go-to
 (fn [[route params]]
   (go-to route params)))

(defn- ref-from-param [param-kw]
  (let [params (params)
        ref (get params param-kw)
        as-int (utils/parse-int ref)]
    (cond
      (not ref) nil
      (pos? as-int) as-int
      (str/starts-with? ref "_") (keyword ref)
      :default ref)))

(rf/reg-event-fx
 ::set
 (fn [{:keys [db]} [_ handler route-params]]
   (merge {:db (assoc db :route [handler route-params])
           :document-title (route-name handler route-params)}
          (when-let [init-fx (:init-fx (find-route handler))]
            {:dispatch init-fx}))))
