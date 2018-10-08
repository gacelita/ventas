(ns ventas.themes.clothing.components.header
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.cart :as cart]
   [ventas.components.search-box :as search-box :refer [search-box]]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]
   [ventas.routes :as routes]
   [ventas.session :as session]
   [ventas.utils.re-frame :refer [pure-subscribe]]
   [ventas.common.utils :refer [find-first]]
   [ventas.components.image :as image]))

(def state-key ::state)

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
                 [::events/users.logout]]}))

(defn on-result-click [{:keys [type slug id]}]
  (when-let [route (case type
                     :product :frontend.product
                     :category :frontend.category
                     nil)]
    (routes/go-to route :id (or slug id))))

(defn header []
  [:div.skeleton-header
   [base/container
    [:div.skeleton-header__search
     [search-box
      {:id ::search-box
       :options (->> @(rf/subscribe [::search-box/items ::search-box])
                     (sort-by :type)
                     (reverse)
                     (search-box/->options on-result-click))
       :on-key-down
       (fn [e]
         (when (= (.-key e) "Enter")
           (when-let [search-query @(rf/subscribe [::search-box/query ::search-box])]
             (routes/go-to :frontend.search :search search-query))))}]]

    [:div.skeleton-header__logo
     (let [{:customization/keys [logo] :as config} @(rf/subscribe [::events/db [:configuration]])]
       [:a {:title (:customization/name config)
            :href (-> js/window (.-location) (.-origin))}
        [:img {:src (if logo
                      (image/get-url logo :logo)
                      "files/logo")}]])]

    [:div.skeleton-header__buttons
     [:div.skeleton-header__profile
      {:on-click #(routes/go-to :frontend.login)
       :on-blur #(rf/dispatch [::close])}
      [base/icon {:name "user"}]
      [:span (i18n ::my-account)]
      (when @(rf/subscribe [::session/identity.valid?])
        [base/menu {:vertical true
                    :class "skeleton-header__user-menu"}
         [base/menu-item {:on-click #(rf/dispatch [::logout])}
          (i18n ::logout)]])]
     [:div.skeleton-header__favorites
      {:on-click #(routes/go-to :frontend.favorites)}
      [base/icon {:name "heart"}]
      [:span (i18n ::my-favorites)]]
     [:div.skeleton-header__cart {:on-click #(routes/go-to :frontend.cart)}
      [base/icon {:name "shopping cart"}]
      [:span (i18n ::my-cart)]
      (let [count @(rf/subscribe [::cart/item-count])]
        (when (pos? count)
          [:div.skeleton-header__cart-count
           @(rf/subscribe [::cart/item-count])]))]]]])
