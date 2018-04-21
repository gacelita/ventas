(ns ventas.email.templates.user-registered
  (:require
   [ventas.email.elements :as elements]
   [ventas.email.templates :as templates]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.i18n :refer [i18n]]))

(defmethod templates/template-body :user-registered [_ {:keys [user]}]
  (let [culture-kw (elements/get-user-culture user)]
    (elements/skeleton
     user
     [:p (i18n culture-kw ::welcome (entities.configuration/get :site.title))]
     [:p (i18n culture-kw ::add-an-address)]
     [:p
      [:a {:href (elements/get-url "/profile")}
       (i18n culture-kw ::go-to-profile)]])))
