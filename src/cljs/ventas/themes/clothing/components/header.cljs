(ns ventas.themes.clothing.components.header
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.i18n :refer [i18n]]
   [ventas.utils :as utils :include-macros true]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

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
                 [::events/session.stop]]}))

(rf/reg-event-fx
 ::search
 (fn [{:keys [db]} [_ search]]
   (if (empty? search)
     {:db (update db state-key #(dissoc % :search))}
     {:db (assoc-in db [state-key :search-query] search)
      :dispatch [::backend/search
                 {:params {:search search}
                  :success [::events/db [state-key :search]]}]})))

(defn- search-result-view [{:keys [id name image type]}]
  [:div.search-result
   (when-let [route (case type
                      :product :frontend.product
                      :category :frontend.category
                      nil)]
     {:on-click #(routes/go-to route :id id)})
   (when image
     [:img {:src (str "images/" (:id image) "/resize/header-search")}])
   [:div.search-result__right
    [:p.search-result__name name]
    [:p.search-result__type (i18n (utils/ns-kw type))]]])

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
                        :selection true
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