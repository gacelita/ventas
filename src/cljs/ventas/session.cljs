(ns ventas.session
  (:require
   [re-frame.core :as rf]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]))

(defn get-identity []
  @(rf/subscribe [::events/db [:session :identity]]))

(defn valid-identity? []
  (let [{:keys [id status]} (get-identity)]
    (and id
         (not= :user.status/unregistered status))))

(rf/reg-event-fx
 ::require-identity
 (fn [_ _]
   (let [{:keys [id status]} (get-identity)]
     (when (or (not id) (= :user.status/unregistered status))
       (merge {:go-to [:frontend.login]}
              (when (= :user.status/unregistered status)
                {:dispatch [::notificator/add {:message (i18n ::unregistered-error)
                                               :theme "error"}]}))))))
