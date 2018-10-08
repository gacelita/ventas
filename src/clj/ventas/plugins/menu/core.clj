(ns ventas.plugins.menu.core
  (:require
   [ventas.server.api :as api]
   [ventas.i18n :refer [i18n]]
   [ventas.common.utils :refer [find-first]]))

(defonce config (atom {}))

(defn setup! [m]
  (reset! config m))

(defn call-fn [kw & args]
  (let [f (get @config kw)]
    (when-not f
      (throw (Exception. "Menu has not been set up")))
    (apply f args)))

(defn find-routes [input culture]
  (call-fn :find-routes input culture))

(defn route->name [route culture]
  (call-fn :route->name route culture))

(api/register-endpoint!
  ::autocompletions.get
  (fn [{{:keys [query]} :params} {:keys [session]}]
    (let [culture (api/get-culture session)]
      (find-routes query culture))))

(api/register-endpoint!
  ::routes.get-name
  (fn [{{:keys [route]} :params} {:keys [session]}]
    (let [culture (api/get-culture session)]
      (route->name route culture))))

(api/register-endpoint!
  ::menu.get
  (fn [{{:keys [id]} :params} {:keys [session]}]
    (api/find-serialize-with-session session id)))