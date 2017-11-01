(ns ventas.components.breadcrumbs
  (:require
   [re-frame.core :as rf]
   [reagent.session :as session]
   [ventas.utils :as util]
   [ventas.routes :as routes]
   [ventas.components.base :as base]))

(defn- breadcrumb-data [current-route route-params]
  (map (fn [route] {:url (apply routes/path-for route (first (seq route-params)))
                    :name (:name (routes/find-route route))
                    :route route})
       (conj (routes/route-parents current-route)
             current-route)))

(defn breadcrumb-view [current-page route-params]
  [base/breadcrumb {:class "breadcrumbs"}
   (util/interpose-fn
    (fn [] [base/breadcrumbDivider {:key (gensym)}])
    (for [breadcrumb (breadcrumb-data current-page route-params)]
      [base/breadcrumbSection
       {:key (:route breadcrumb)
        :class "breadcrumbs__breadcrumb"
        :href (:url breadcrumb)}
       (:name breadcrumb)]))])

(defn breadcrumbs []
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    (breadcrumb-view current-page route-params)))