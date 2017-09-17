(ns ventas.themes.mariscosriasbajas.pages.login
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [fqcss.core :refer [wrap-reagent]]
            [re-frame-datatable.core :as dt]
            [ventas.utils.logging :refer [trace debug info warn error]]
            [ventas.page :refer [pages]]
            [ventas.routes :refer [route-parents routes]]
            [ventas.components.notificator :as ventas.notificator]
            [ventas.components.popup :as ventas.popup]
            [ventas.components.category-list :refer [category-list]]
            [ventas.components.product-list :refer [products-list]]
            [ventas.components.cart :as ventas.cart]
            [ventas.themes.mariscosriasbajas.components.header :refer [header]]
            [ventas.themes.mariscosriasbajas.components.skeleton :refer [skeleton]]
            [ventas.themes.mariscosriasbajas.components.preheader :refer [preheader]]
            [ventas.themes.mariscosriasbajas.components.heading :as theme.heading]
            [ventas.util :as util :refer [value-handler]]
            [ventas.plugin :as plugin]
            [soda-ash.core :as sa]))

(defn login-successful [{:keys [user token]}]
  (js/console.log "user" user "token" token)
  (rf/dispatch [:app/notifications.add {:message "Sesión iniciada"}])
  (rf/dispatch [::login-success user token]))

(rf/reg-event-fx
  ::login-success
  [(rf/inject-cofx :local-storage)]
  (fn [{:keys [db local-storage]} [_ user token]]
    {:db (assoc db :session user)
     :local-storage (assoc local-storage :token token)}))

(rf/reg-event-fx
  ::login
  (fn [cofx [_ {:keys [email password]}]]
    {:ws-request {:name :users/login
                  :params {:email email
                           :password password}
                  :success-fn login-successful}}))

(rf/reg-event-fx
 ::register
 (fn [cofx [_ {:keys [name email password]}]]
   {:ws-request {:name :users/register
                 :params {:name name
                          :email email
                          :password password}
                 :success-fn #(rf/dispatch [:app/notifications.add {:message "Usuario registrado"}])}}))

(defn login []
  (reagent/with-let [data (atom {})]
    (wrap-reagent
     [sa/Segment {:fqcss [::segment]}
      [:h3 "Iniciar sesión"]
      [sa/Form
       [sa/FormField
        [:input {:placeholder "Email" :on-change (value-handler #(swap! data assoc :email %))}]]
       [sa/FormField
        [:input {:placeholder "Contraseña" :type "password" :on-change (value-handler #(swap! data assoc :password %))}]]
       [:a {:fqcss [::forgot-password] :href "/"} "¿Olvidaste tu contraseña?"]
       [sa/Button {:type "button" :on-click #(rf/dispatch [::login @data])} "Iniciar sesión"]]])))

(defn register []
  (reagent/with-let [data (atom {})]
    (wrap-reagent
     [sa/Segment {:fqcss [::segment]}
      [:h3 "Registro"]
      [sa/Form
       [sa/FormField
        [:input {:placeholder "Nombre completo"
                 :on-change (value-handler #(swap! data assoc :name %))}]]
       [sa/FormField
        [:input {:placeholder "Email"
                 :on-change (value-handler #(swap! data assoc :email %))}]]
       [sa/FormField
        [:input {:placeholder "Contraseña"
                 :type "password"
                 :on-change (value-handler #(swap! data assoc :password %))}]]
       [sa/Button {:type "button" :on-click #(rf/dispatch [::register @data])}
        "Registro"]]])))

(defmethod pages :frontend.login []
  [skeleton
   (wrap-reagent
    [:div {:fqcss [::page]}
     (let [session @(rf/subscribe [:session])]
       (if (seq session)
         [sa/Container
          [:p "Bienvenido a tu cuenta " (:name session) ". Desde aquí puedes administrar tus direcciones y pedidos."]
          [sa/Button
           [sa/Icon {:name "user"}]
           "Mi perfil"]
          [sa/Button
           [sa/Icon {:name "unordered list"}]
           "Mis pedidos"]
          [sa/Button
           [sa/Icon {:name "address book"}]
           "Mis direcciones"]]
         [sa/Container
          [login]
          [register]]))])])