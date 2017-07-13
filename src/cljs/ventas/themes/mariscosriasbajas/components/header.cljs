(ns ventas.themes.mariscosriasbajas.components.header
  (:require [fqcss.core :refer [wrap-reagent]]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [ventas.routes :as routes :refer [go-to]]
            [ventas.util :as util]))

(rf/reg-sub
 :resources/logo
 (fn [db _] (-> db :resources :logo)))

(rf/reg-event-fx
 :resources/logo
 (fn [cofx [_]]
   {:ws-request {:name :resources/find
                 :success-fn #(rf/dispatch [:app/entity-query.next [:resources :logo] %])}}))

(rf/reg-sub
 ::opened
 (fn [db _]
   (-> db ::opened)))

(rf/reg-event-db
 ::toggle
 (fn [db [_]]
   (update db ::opened not)))

(rf/reg-event-db
 ::close
 (fn [db [_]]
   (assoc db ::opened false)))

(defn header []
  [:div
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
          [:button {:on-click #(go-to :frontend.cart)} [sa/Icon {:name "add to cart"}] "Mi cesta"]
          [:button {:on-click #(go-to :frontend.login) :on-blur #(rf/dispatch [::close])} [sa/Icon {:name "user"}] "Mi cuenta"
           [sa/Icon {:name "caret down" :on-click (fn [e] (-> e .stopPropagation) (rf/dispatch [::toggle]))}]
           [sa/Menu {:vertical true :fqcss [::user-menu] :class (if @(rf/subscribe [::opened]) "visible" "unvisible")}
            [sa/MenuItem {:on-click #(rf/dispatch [:app/session.stop])} "Cerrar sesión"]]]
          ]]]]))])