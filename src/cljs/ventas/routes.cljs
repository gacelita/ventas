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
   [ventas.utils.logging :as log]
   [ventas-bidi-syntax.core :as bidi-syntax]))

(def ^:private route-data
  (atom []))

(def ^:private routes (atom (bidi-syntax/to-bidi @route-data)))


(defn define-route! [name {:keys [component] :as attrs}]
  (swap! route-data conj (assoc attrs :handler name))
  (reset! routes (bidi-syntax/to-bidi @route-data))
  (when component
    (defmethod page/pages name []
      [component])))

(defn define-routes! [new-routes]
  (swap! route-data concat new-routes)
  (reset! routes (bidi-syntax/to-bidi @route-data)))

(defn find-route
  "Finds a route by its id"
  [id]
  (first (filter #(= (:handler %) id) @route-data)))

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
   (when route
     (go-to route params))))

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
           :document-title (route-name handler route-params)
           :ws-request {:name :stats.navigation
                        :params {:handler handler
                                 :params route-params}}}
          (when-let [init-fx (:init-fx (find-route handler))]
            {:dispatch init-fx}))))
