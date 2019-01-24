(ns ventas.email.templates.order-status-changed
  (:require
   [slingshot.slingshot :refer [throw+]]
   [ventas.database.entity :as entity]
   [ventas.email.elements :as elements]
   [ventas.email.templates :as templates]
   [ventas.entities.product :as entities.product]
   [ventas.i18n :refer [i18n]]
   [ventas.entities.order :as entities.order]
   [ventas.entities.user :as entities.user]))

(defn amount->str [{:amount/keys [value currency]}]
  (str value " " (:currency/symbol (entity/find currency))))

(defn- order-line [user {:order.line/keys [quantity product-variation]}]
  (let [{:product/keys [name price] :as product} (entities.product/normalize-variation product-variation)]
    [:tr
     [:td.order__image
      [:img {:width 20
             :src (elements/get-url (str "/images/" (:db/id product) "/resize/cart-page-line"))}]]
     [:td {:align "left"} (entity/find-serialize name {:culture (:user/culture user)})]
     [:td {:align "right"} quantity]
     [:td {:align "right"} (amount->str (entity/find price))]]))

(defmethod templates/template :order-status-changed [_ {:keys [user order]}]
  (let [culture-kw (entities.user/get-culture user)
        {:order/keys [shipping-address lines status] :db/keys [id]} order]
    {:subject (i18n culture-kw ::subject status id)
     :body
     (elements/skeleton
      user
      [:p (i18n culture-kw ::heading status id)]
      [:br]
      (elements/table
       {:width "100%"}
       [:tr
        (elements/header
         {:colSpan 2
          :align "left"}
         (i18n culture-kw ::product))
        (elements/header {:align "right"} (i18n culture-kw ::quantity))
        (elements/header {:align "right"} (i18n culture-kw ::amount))]
       (map #(->> (entity/find %) (order-line user))
            lines)
       [:tr
        [:td.order__total
         {:align "right"
          :colSpan 3}
         [:strong (i18n culture-kw ::total-amount)]]
        [:td {:align "right"}
         (when-let [amount (entities.order/get-amount order)]
           [:strong (amount->str amount)])]])
      [:br]
      [:h4 (i18n culture-kw ::shipping-address)]
      (let [{:keys [first-name last-name address address-second-line
                    zip city state country]} (entity/serialize (entity/find shipping-address)
                                                               {:culture (:user/culture user)})]
        [:div.address-content
         [:span first-name " " last-name]
         [:br]
         [:span address " " address-second-line]
         [:br]
         [:span zip " " city " " (:name state)]
         [:br]
         [:span (:name country)]])
      [:br]
      [:p (i18n culture-kw ::go-to-orders)
       " "
       [:a {:href (elements/get-url "/profile/orders")}
        (i18n culture-kw ::go-to-orders-link)]])}))
