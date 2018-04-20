(ns ventas.email.templates.password-forgotten
  (:require
   [ventas.email.templates :as templates]
   [ventas.email.elements :as elements]
   [ventas.i18n :refer [i18n]]
   [clojure.string :as str]
   [ventas.auth :as auth]))

(defmethod templates/template-body :password-forgotten [_ {:keys [user]}]
  (let [culture-kw (elements/get-user-culture user)]
    (elements/skeleton
     user
     [:p
      [:a {:href (elements/get-url (str "/profile/account?token=" (auth/user->token user)))}
       (i18n culture-kw ::reset-your-password)]])))