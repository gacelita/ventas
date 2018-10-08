(ns ventas.themes.clothing.components.skeleton
  (:require
   [ventas.components.base :as base]
   [ventas.components.breadcrumbs :as breadcrumbs]
   [ventas.components.cookies :as cookies]
   [ventas.components.notificator :as notificator]
   [ventas.components.popup :as popup]
   [ventas.i18n :refer [i18n]]
   [ventas.events :as events]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.footer :as footer]
   [ventas.themes.clothing.components.header :as header]
   [ventas.themes.clothing.components.menu :as menu]
   [ventas.themes.clothing.components.preheader :as preheader]
   [re-frame.core :as rf]
   [ventas.components.image :as image]
   [clojure.string :as str]))

(def css-template
  "html body,
   html a,
   html .menu__item a,
   html .preheader__item a,
   html .skeleton-header__buttons > div,
   html .product-list__content,
   html .product-list__actions,
   html .product-page__name,
   html .product-page__price,
   html .heading__text h3,
   html .ui.selection.dropdown,
   html .category-list__name {
    color: {{foreground-color}};
   }
   html .product-page__add-to-cart {
     background-color: {{foreground-color}};
   }
   .root {
     background-color: {{background-color}};
   }")

(defn skeleton [contents]
  [:div.root
   [:style
    (let [{:customization/keys [foreground-color background-color]} @(rf/subscribe [::events/db [:configuration]])]
      (-> css-template
          (str/replace "{{foreground-color}}" foreground-color)
          (str/replace "{{background-color}}" background-color)))]
   [notificator/notificator]
   [popup/popup]
   [cookies/cookies (i18n ::cookies)]
   [:div.root__wrapper
    [preheader/preheader]
    [header/header]
    [menu/menu]
    (when-not (= (routes/handler) :frontend)
      [:div
       (let [{:customization/keys [header-image]} @(rf/subscribe [::events/db [:configuration]])]
         [:div.breadcrumbs-wrapper
          (when header-image
            {:style {:backgroundImage (str "url(" (image/get-url header-image) ")")}
             :class "breadcrumbs-wrapper--with-header-image"})
          [base/container
           [breadcrumbs/breadcrumbs]]])
       [base/divider]])
    [:div.page-wrapper
     contents]
    [footer/footer]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::cookies/get-state-from-local-storage]]}))