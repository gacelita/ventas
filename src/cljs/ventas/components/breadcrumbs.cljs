(ns ventas.components.breadcrumbs
  (:require [fqcss.core :refer [wrap-reagent]]
            [soda-ash.core :as sa]
            [re-frame.core :as rf]
            [reagent.session :as session]
            [ventas.util :as util]))

(defn breadcrumbs []
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    (wrap-reagent
     [sa/Breadcrumb {:fqcss [::breadcrumbs]}
      (util/interpose-fn
       (fn [] [sa/BreadcrumbDivider {:key (gensym)}])
       (for [breadcrumb (util/breadcrumbs current-page route-params)]
         [sa/BreadcrumbSection {:key (:route breadcrumb) :fqcss [::breadcrumb] :href (:url breadcrumb)}
          (:name breadcrumb)]))])))