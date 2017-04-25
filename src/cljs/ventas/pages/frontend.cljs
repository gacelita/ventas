(ns ventas.pages.frontend
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [taoensso.timbre :as timbre :refer-macros [trace debug info warn error]]
            [ventas.pages.interface :refer [pages]]
            [ventas.routes :refer [route-parents routes]]
            [ventas.components.notificator]
            [ventas.util :as util]
            [soda-ash.core :as sa]))

(defn skeleton [contents]
  (info "Rendering...")
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    [:div {:class "bu root"}
      ; [bu-debugger]
      [ventas.components.notificator/bu-notificator]
      [:div {:class "bu wrapper"}
        [sa/Menu {:vertical true :inverted true :sticky true :class "bu menu"}
          [sa/MenuItem
            "Administración"
            [sa/MenuMenu
              (for [item [{:route :backend :name "Inicio"}
                          {:route :backend.users :name "Usuarios"}
                          {:route :backend.playground :name "Dev playground"}]]
                ^{:key (:route item)} [sa/MenuItem {:link true :href (bidi/path-for routes (:route item))} (:name item)])]]]
        [sa/Container {:class "bu main"}
          [sa/Breadcrumb
            (util/interpose-fn (fn [] [sa/BreadcrumbDivider {:key (util/gen-key)}])
              (for [breadcrumb (util/breadcrumbs current-page route-params)]
                [sa/BreadcrumbSection {:key (:route breadcrumb) :href (:url breadcrumb)} (:name breadcrumb)]))]
          [sa/Divider]
          ^{:key current-page} contents]]]))

(defmethod pages :frontend []
  [skeleton
    [:h2 "Test frontend"]])

