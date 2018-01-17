(ns ventas.themes.clothing.components.header
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

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

(rf/reg-event-fx
 ::logout
 (fn [_ _]
   {:dispatch-n [[::toggle]
                 [::events/session.stop]]}))

(defn header []
  (rf/dispatch [::events/configuration.get :site.title])
  (fn []
    [:div.skeleton-header
     [:div.ui.container

      [:div.skeleton-header__logo
       (let [title @(rf/subscribe [::events/db [:configuration :site.title]])]
         [:a {:title (:value title)
              :href (-> js/window (.-location) (.-origin))}
          [:img {:src "files/logo"}]])]

      [:div.skeleton-header__right
       [:div.skeleton-header__search
        [base/dropdown {:placeholder (i18n ::search)
                        :search true
                        :selection true
                        :options []
                        :on-change #(rf/dispatch [::search (-> % .-target .-value)])}]
        #_[base/form-input
         {:placeholder (i18n ::search)
          :on-change #(rf/dispatch [::search (-> % .-target .-value)])}]]
       [:div.skeleton-header__buttons

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
                     :class (str "skeleton-header__user-menu "
                                 (if @(rf/subscribe [::opened])
                                   "visible"
                                   "unvisible"))}
          [base/menu-item {:on-click #(rf/dispatch [::logout])}
           (i18n ::logout)]]]]]]]))