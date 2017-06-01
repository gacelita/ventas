(ns ventas.themes.mariscosriasbajas.components.header
  (:require [fqcss.core :refer [wrap-reagent]]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [ventas.util :as util]))

(rf/reg-sub :resources/logo
  (fn [db _] (-> db :resources :logo)))

(rf/reg-event-fx :resources/logo
  (fn [cofx [_]]
    {:ws-request {:name :resources/find
                  :success-fn #(rf/dispatch [:app/entity-query.next [:resources :logo] %])}}))

(defn header []
  (reagent/with-let [sub-logo (util/sub-resource-url :logo)
                     sub-title (util/sub-configuration :site-title)]
    (wrap-reagent
      [:div.ventas {:fqcss [::header]}
        [:div.ui.container
          [:div.ventas {:fqcss [::header-logo]}
            [:a {:title (get-in @(rf/subscribe [sub-title]) [:value]) :href (-> js/window (.-location) (.-origin))}
              [:img {:src (get-in @(rf/subscribe [sub-logo]) [:file :url])}]]]
          [:div.ventas {:fqcss [::header-right]}
           [:div.ventas {:fqcss [::header-info]}
            [:div.ventas {:fqcss [::header-info-shipping]}
             [:strong "ENVÍOS" [:br] "GRATIS"]]
            [:div.ventas {:fqcss [::header-info-from]}
             "a partir" [:br] "de " [:strong "130 €"]]]
           [:div.ventas {:fqcss [::header-buttons]}]]]])))