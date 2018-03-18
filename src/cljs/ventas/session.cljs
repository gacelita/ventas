(ns ventas.session
  (:require
   [cljs.core.async :refer [chan >! go]]
   [re-frame.core :as rf]
   [ventas.components.notificator :as notificator]
   [ventas.events :as events]
   [ventas.i18n :refer [i18n]]))

(def ready
  "A value will be put here when the session is ready"
  (chan))

(rf/reg-event-fx
 ::listen-to-events
 (fn [_ _]
   {:forward-events {:register ::listener
                     :events #{::events/session.start ::events/session.error}
                     :dispatch-to [::session-done]}}))

(rf/dispatch [::listen-to-events])

(rf/reg-event-fx
 ::session-done
 (fn [_ [_ [event-kw message]]]
   (go (>! ready {:success (= event-kw ::events/session.start)
                  :message message}))
   {:forward-events {:unregister ::listener}}))

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
