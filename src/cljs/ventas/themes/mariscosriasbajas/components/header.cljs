(ns ventas.themes.mariscosriasbajas.components.header
  (:require [fqcss.core :refer [wrap-reagent]]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [soda-ash.core :as sa]
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
      [:div {:fqcss [::header]}
        [:div.ui.container
          [:div {:fqcss [::header-logo]}
            [:a {:title (get-in @(rf/subscribe [sub-title]) [:value]) :href (-> js/window (.-location) (.-origin))}
              [:img {:src (get-in @(rf/subscribe [sub-logo]) [:file :url])}]]]
          [:div {:fqcss [::header-right]}
            [:div {:fqcss [::header-info]}
              [:div {:fqcss [::header-info-shipping]}
                [:strong "ENVÍOS" [:br] "GRATIS"]]
              [:div {:fqcss [::header-info-from]}
                "a partir" [:br] "de " [:strong "130 €"]]]
           [:div {:fqcss [::header-buttons]}
              [:button [sa/Icon {:name "add to cart"}] "Mi cesta"]
              [:button [sa/Icon {:name "user"}] "Mi cuenta"]
            ]]]])))