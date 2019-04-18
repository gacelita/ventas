(ns ventas.session
  (:require
   [cljs.core.async :as core.async :refer [>! chan go]]
   [day8.re-frame.forward-events-fx]
   [re-frame.core :as rf]))

(def ready
  "A value will be put here when the session is ready"
  (chan))

(rf/reg-event-fx
 ::listen-to-events
 (fn [_ _]
   {:forward-events {:register ::listener
                     :events #{:ventas.events/session.start :ventas.events/session.error}
                     :dispatch-to [::session-done]}}))

(rf/dispatch [::listen-to-events])

(rf/reg-event-fx
 ::session-done
 (fn [_ [_ [event-kw message]]]
   (core.async/put! ready {:success (= event-kw :ventas.events/session.start)
                           :message message})
   {:forward-events {:unregister ::listener}}))

(defn- get-identity [db]
  (get-in db [:session :identity]))

(rf/reg-sub ::identity get-identity)

(rf/reg-sub
 ::culture
 :<- [::identity]
 (fn [identity]
   (:culture identity)))

(rf/reg-sub
 ::culture-keyword
 :<- [::culture]
 (fn [culture]
   (:keyword culture)))

(rf/reg-sub
 ::culture-id
 :<- [::culture]
 (fn [culture]
   (:id culture)))

(defn- identity-valid? [{:keys [id status]}]
  (and id
       (not= :user.status/unregistered status)))

(rf/reg-sub
 ::identity.valid?
 (fn [_]
   (rf/subscribe [::identity]))
 identity-valid?)

