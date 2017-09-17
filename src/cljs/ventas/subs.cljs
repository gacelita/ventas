(ns ventas.subs
  (:require
    [re-frame.core :as rf]
    [clojure.pprint :as pprint]
    [ventas.subscriptions.products]))

;; Debugging

(rf/reg-sub :all
  (fn [db _] (pprint/write db :stream nil)))

;; Test subscriptions

(rf/reg-sub :messages
  (fn [db _] (-> db :messages)))

(rf/reg-sub :session
  (fn [db _] (-> db :session)))

(rf/reg-sub :app/form
  (fn [db _] (-> db :form)))

(rf/reg-sub :admin.users.edit/comment-modal
  (fn [db _] (-> db :comment-modal)))

(rf/reg-sub :admin.users.edit/made-comment-modal
  (fn [db _] (-> db :made-comment-modal)))

;; Application subscriptions

(rf/reg-sub :app.users/users
  (fn [db _] (-> db :users)))

(rf/reg-sub :admin.users.edit/comments
  (fn [db _] (-> db :form :comments)))

(rf/reg-sub :admin.users.edit/made-comments
  (fn [db _](-> db :form :made-comments)))

(rf/reg-sub :admin.users.edit/friends
  (fn [db _] (-> db :form :friends)))

(rf/reg-sub :admin.users.edit/own-images
  (fn [db _] (-> db :form :own-images)))

(rf/reg-sub :admin.users.edit/images
  (fn [db _] (-> db :form :images)))

(rf/reg-sub :app.reference/user.role
  (fn [db _] (-> db :reference :user.role)))