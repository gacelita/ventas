(ns ventas.plugins.api.core
  (:require
   [cljs.pprint :as pprint]
   [cljs.reader :as reader]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.plugins.api.backend :as backend]
   [ventas.routes :as routes]
   [ventas.utils :as utils]
   [reagent.core :as reagent]))

(def state-key ::state)

(defn pprint-str [v]
  (with-out-str
   (pprint/pprint v)))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch [::backend/describe
               {:success [::events/db [state-key :description]]}]}))

(rf/reg-event-fx
 ::submit
 (fn [{:keys [db]} [_ request]]
   (let [params (get-in db [state-key :params request])]
     (try
       {:ws-request {:name request
                     :params (reader/read-string params)
                     :success [::submit.next request true]
                     :error [::submit.next request false]}}
       (catch :default e
         {:db (assoc-in db [state-key :results request]
                        {:success? false
                         :data (str "Error making the request:\n"
                                    (pprint-str e))})})))))

(rf/reg-event-db
 ::submit.next
 (fn [db [_ request success? result]]
   (assoc-in db
             [state-key :results request]
             {:success? success?
              :data result})))

(rf/reg-event-fx
 ::generate-params
 (fn [{:keys [db]} [_ request]]
   (when-let [spec (get-in db [state-key :description request])]
     {:dispatch [::backend/generate-params
                 {:params {:request request}
                  :success #(rf/dispatch [::events/db
                                          [state-key :params request]
                                          (pprint-str %)])}]})))

(defn- request-view [request {:keys [spec doc]}]
  (let [{:keys [data success?]} @(rf/subscribe [::events/db [state-key :results request]])]
    [base/segment {:class "api-request"}
     [:p.api-request__name request]
     (when doc
       [:pre.api-request__doc doc])
     [:pre.api-request__spec
      (if spec
        (pprint-str spec)
        [:p "No spec defined"])]
     [:div.api-request__params
      (let [params @(rf/subscribe [::events/db [state-key :params request]])]
        [:textarea {:placeholder "Request parameters"
                    :value params
                    :on-change #(rf/dispatch [::events/db [state-key :params request]
                                              (-> % .-target .-value)])}])]
     [:div.api-request__actions
      [base/button {:on-click #(rf/dispatch [::submit request])}
       "Submit"]
      (when spec
        [base/button {:on-click #(rf/dispatch [::generate-params request])}
         "Generate parameters"])
      (when data
        [base/button {:on-click #(rf/dispatch [::events/db [state-key :results request] nil])}
         "Clear"])]
     (when data
       [:pre.api-request__result {:class (str "api-request__result--" (if success?
                                                                        "success"
                                                                        "error"))}
        (if (string? data)
          data
          (pprint-str data))])]))

(defn- content []
  [:div
   (let [description (->> @(rf/subscribe [::events/db [state-key :description]])
                          (sort-by first))]
     (for [[request data] description]
       ^{:key request}
       [request-view request data]))])

(defn- login-form []
  (reagent/with-let [data (atom {})]
    (let [{:keys [identity]} @(rf/subscribe [::events/db [:session]])]
      (if identity
        [:div
         [:p "Current session:"]
         [:pre (pprint-str identity)]
         [base/button {:type "button"
                       :on-click #(rf/dispatch [::events/users.logout])}
          "Logout"]]
        [:div
         [:p
          [:strong "Warning: you are not authenticated."]
          [:br]
          "Some requests may require you to have a session or be an administrator. Keep this in mind when trying out the requests."]
         [base/form {:class "api-page__login"}
          [base/form-field
           [:input {:placeholder "Email"
                    :on-change (utils/value-handler #(swap! data assoc :email %))}]]
          [base/form-field
           [:input {:placeholder "Password"
                    :type "password"
                    :on-change (utils/value-handler #(swap! data assoc :password %))}]]
          [base/button {:type "button"
                        :on-click #(rf/dispatch [::events/users.login @data])}
           "Login"]]]))))

(defn page []
  (let [handler (routes/handler)]
    [:div.api-page
     [base/container {:class "bu main"}
      [base/header {:as "h1"}
       "ventas websocket API description"]
      [base/divider]
      [login-form]
      [base/divider]
      [content]]]))

(routes/define-route!
 :api
 {:name "Websocket API documentation"
  :url "api"
  :component page
  :init-fx [::init]})