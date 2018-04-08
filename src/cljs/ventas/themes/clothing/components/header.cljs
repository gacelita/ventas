(ns ventas.themes.clothing.components.header
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.components.cart :as cart]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.i18n :refer [i18n]]
   [ventas.session :as session]
   [ventas.routes :as routes]
   [ventas.utils :as utils :include-macros true]))

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

(rf/reg-event-fx
 ::search
 (fn [{:keys [db]} [_ search]]
   (if (empty? search)
     {:db (update db state-key #(dissoc % :search))}
     {:db (assoc-in db [state-key :search-query] search)
      :dispatch [::backend/search
                 {:params {:search search}
                  :success [::events/db [state-key :search]]}]})))

(defn- search-result-view [{:keys [id name image type slug]}]
  [:div.search-result
   (when-let [route (case type
                      :product :frontend.product
                      :category :frontend.category
                      nil)]
     {:on-click #(routes/go-to route :id (or slug id))})
   (when image
     [:img {:src (str "images/" (:id image) "/resize/header-search")}])
   [:div.search-result__right
    [:p.search-result__name name]
    [:p.search-result__type (i18n (utils/ns-kw type))]]])

(defn header
  "@TODO Remove form-2 dispatch antipattern"
  []
  (rf/dispatch [::events/configuration.get #{:site.title}])
  (fn []
    [:div.skeleton-header
     [base/container
      [:div.skeleton-header__search
       [base/dropdown {:placeholder (i18n ::search)
                       :selection true
                       :icon "search"
                       :on-key-down (fn [e] (when (= (.-key e) "Enter")
                                              (routes/go-to :frontend.search
                                                            :search
                                                            @(rf/subscribe [::events/db [state-key :search-query]]))))
                       :search (fn [options _] options)
                       :options (->> @(rf/subscribe [::events/db [state-key :search]])
                                     (sort-by :type)
                                     (reverse)
                                     (map #(reagent/as-element [search-result-view %])))
                       :on-search-change #(rf/dispatch [::search (-> % .-target .-value)])}]]

      [:div.skeleton-header__logo
       (let [title @(rf/subscribe [::events/db [:configuration :site.title]])]
         [:a {:title title
              :href (-> js/window (.-location) (.-origin))}
          [:img {:src "files/logo"}]])]

      [:div.skeleton-header__buttons
       [:div.skeleton-header__profile
        {:on-click #(routes/go-to :frontend.login)
         :on-blur #(rf/dispatch [::close])}
        [base/icon {:name "user"}]
        [:span (i18n ::my-account)]
        (when (session/valid-identity?)
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
             @(rf/subscribe [::cart/item-count])]))]]]]))
