(ns ventas.session
  (:require
   [cljs.core.async :as core.async :refer [>! chan go]]
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
   (core.async/put! ready {:success (= event-kw ::events/session.start)
                           :message message})
   {:forward-events {:unregister ::listener}}))

(defn- get-identity [db]
  (get-in db [:session :identity]))

(rf/reg-sub ::identity get-identity)

(defn- identity-valid? [{:keys [id status]}]
  (and id
       (not= :user.status/unregistered status)))

(rf/reg-sub
 ::identity.valid?
 (fn [_]
   (rf/subscribe [::identity]))
 identity-valid?)

(rf/reg-event-fx
 ::require-identity
 (fn [{:keys [db]} _]
   (let [{:keys [status] :as identity} (get-identity db)]
     (when-not (identity-valid? identity)
       (merge {:go-to [:frontend.login]}
              (when (= :user.status/unregistered status)
                {:dispatch [::notificator/add {:message (i18n ::unregistered-error)
                                               :theme "error"}]}))))))
