(ns ventas.themes.mariscosriasbajas.components.header
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]))

(rf/reg-sub
 :resources/logo
 (fn [db _] (-> db :resources :logo)))

(rf/reg-event-fx
 :resources/logo
 (fn [cofx [_]]
   {:dispatch [:api/resources.get
               {:params {:key :logo}
                :success-fn #(rf/dispatch [:ventas/db [:resources :logo] %])}]}))

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
  (rf/dispatch [:ventas/configuration.get :site-title])
  (rf/dispatch [:ventas/resources.get :logo])
  (fn []
    [:div.header
     [:div.ui.container
      [:div.header__logo
       (let [title @(rf/subscribe [:ventas.db [:configuration :site-title]])
             logo @(rf/subscribe [:ventas.db [:resources :logo]])]
         [:a {:title (:value title)
              :href (-> js/window (.-location) (.-origin))}
          [:img {:src (get-in logo [:file :url])}]])]
      [:div.header__right
       [:div.header__info
        [:div.header__shipping
         [:strong (i18n ::free-shipping)]]
        [:div.header__from
         (i18n ::from-130-euro)]]
       [:div.header__buttons
        [:button {:on-click #(routes/go-to :frontend.cart)}
         [base/icon {:name "add to cart"}]
         (i18n ::my-cart)]
        [:button {:on-click #(routes/go-to :frontend.login)
                  :on-blur #(rf/dispatch [::close])}
         [base/icon {:name "user"}]
         (i18n ::my-account)
         [base/icon {:name "caret down"
                     :on-click (fn [e] (-> e (.stopPropagation))
                                 (rf/dispatch [::toggle]))}]
         [base/menu {:vertical true
                     :class (str "header__user-menu "
                                 (if @(rf/subscribe [::opened])
                                   "visible"
                                   "unvisible"))}
          [base/menuItem {:on-click #(rf/dispatch [:ventas/session-stop])}
           (i18n ::logout)]]]]]]]))