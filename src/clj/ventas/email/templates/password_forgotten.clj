(ns ventas.email.templates.password-forgotten
  (:require
   [clojure.string :as str]
   [ventas.auth :as auth]
   [ventas.email.elements :as elements]
   [ventas.email.templates :as templates]
   [ventas.i18n :refer [i18n]]))

(defmethod templates/template :password-forgotten [_ {:keys [user]}]
  {:body
   (let [culture-kw (elements/get-user-culture user)]
     (elements/skeleton
      user
      [:p
       [:a {:href (elements/get-url (str "/profile/account?token=" (auth/user->token user)))}
        (i18n culture-kw ::reset-your-password)]]))})
