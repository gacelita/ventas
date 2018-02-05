(ns ventas.components.breadcrumbs
  (:require
   [re-frame.core :as rf]
   [ventas.utils :as util]
   [ventas.routes :as routes]
   [ventas.components.base :as base]))

(defn- breadcrumb-data [handler params]
  (map (fn [route] {:url (apply routes/path-for route (first (seq params)))
                    :name (routes/route-name route params)
                    :route route})
       (conj (routes/route-parents handler)
             handler)))

(defn breadcrumb-view [handler params]
  [base/breadcrumb {:class "breadcrumbs"}
   (doall
    (util/interpose-fn
     (fn [] [base/breadcrumb-divider {:key (gensym)}])
     (for [{:keys [name url route]} (breadcrumb-data handler params)]
       [base/breadcrumb-section
        {:key route
         :class "breadcrumbs__breadcrumb"
         :href url}
        name])))])

(defn breadcrumbs []
  (let [[handler params] (routes/current)]
    [breadcrumb-view handler params]))
