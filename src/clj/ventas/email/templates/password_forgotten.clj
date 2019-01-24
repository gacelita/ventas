(ns ventas.email.templates.password-forgotten
  (:require
   [ventas.auth :as auth]
   [ventas.email.elements :as elements]
   [ventas.email.templates :as templates]
   [ventas.i18n :refer [i18n]]
   [ventas.entities.user :as entities.user]))

(defmethod templates/template :password-forgotten [_ {:keys [user]}]
  {:body
   (let [culture-kw (entities.user/get-culture user)]
     (elements/skeleton
      user
      [:p
       [:a {:href (elements/get-url (str "/profile/account?token=" (auth/user->token user)))}
        (i18n culture-kw ::reset-your-password)]]))})
