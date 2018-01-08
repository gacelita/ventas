(ns ventas.routes
  "Bidi wrapper and route utilities"
  (:require
   [clojure.string :as str]
   [bidi.bidi :as bidi]
   [ventas.utils.logging :refer [error]]
   [accountant.core :as accountant]
   [ventas.page :as page]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [reagent.ratom :as ratom]
   [ventas.i18n :refer [i18n]]
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
      (error "Route not found" args))
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
  (session/get :route))

(defn params
  "Returns the current route params"
  []
  (:route-params (current)))

(defn route-name
  "Returns the name of a route"
  [kw & [route-params]]
  {:pre [(or (nil? route-params) (map? route-params))]}
  (let [{:keys [name]} (find-route kw)]
    (cond
      (string? name) name
      (keyword? name) (apply i18n name route-params)
      (fn? name) (name route-params)
      (vector? name) @(rf/subscribe name)
      :else (log/warn "A route returns a name that is not a string, keyword, function or vector" kw name))))

(rf/reg-fx
 :go-to
 (fn [[route params]]
   (go-to route params)))