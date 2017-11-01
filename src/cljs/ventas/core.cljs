(ns ventas.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [re-frame.loggers :as rf.loggers]
            [bidi.bidi :as bidi]
            [accountant.core :as accountant]
            [ventas.utils.logging :refer [trace debug info warn error]]
            [cljs.core.async :refer [<! >! put! close! timeout chan]]
            [ventas.api :as api]
            [ventas.ws :as ws]
            [ventas.subs :as subs]
            [ventas.util :as util :refer [dispatch-page-event]]
            [ventas.local-storage :as storage]
            [ventas.devcards.core]

            [ventas.routes :as routes :refer [go-to]]
            [ventas.page :as p]

            [ventas.plugins.core]
            [ventas.pages.admin]
            [ventas.pages.admin.users]
            [ventas.pages.admin.users.edit]
            [ventas.pages.admin.products]
            [ventas.pages.admin.products.edit]
            [ventas.pages.admin.plugins]
            [ventas.pages.datadmin]
            [ventas.pages.api]
            [ventas.themes.mariscosriasbajas.core])
  (:require-macros
    [cljs.core.async.macros :refer (go go-loop)]))

(enable-console-print!)

(rf.loggers/set-loggers!
 {:warn (fn [& args]
          (cond
            (= "re-frame: overwriting" (first args)) nil
            :else (apply ventas.utils.logging/warn args)))
  :log (fn [& args] (apply ventas.utils.logging/info args))
  :error (fn [& args] (apply ventas.utils.logging/error args))
  :group (fn [& args] (apply ventas.utils.logging/info args))})

(storage/reg-co-fx!
 :ventas
 {:fx :local-storage
  :cofx :local-storage})


;; Effects

(rf/reg-fx :go-to
  (fn effect-go-to [data]
    (go-to (get data 0) (get data 1))))


;; Events

;; TODO: Upload, subscription, CLJS modules, autocomplete

(rf/reg-event-db :app/entity-update.next
  (fn [db [_ where what]]
    (debug "entity-update, where: " where)
    (assoc-in db where (map #(if (= (:id %1) (:id what)) what %1) (get-in db where)))))

(rf/reg-event-fx :app/entity-remove
  (fn [cofx [_ data key-vec]]
    {:ws-request {:name :entities.remove 
                  :params data 
                  :success-fn #(rf/dispatch [:app/entity-remove.next key-vec (:id data)])}}))

(rf/reg-event-db :app/entity-remove.next
  (fn [db [_ where what]]
    (debug "entity-remove, where: " where ", what: " what ", where value: " (get-in db where))
    (assoc-in db where (filter #(not (= (:id %1) what)) (get-in db where)))))

(rf/reg-event-fx :app/upload
  (fn [cofx [_ {:keys [source file]}]]
    (let [fr (js/FileReader.)]
      (set! (.-onload fr) #(rf/dispatch [:effects/ws-upload-request {:name :upload
                                                                     :upload-key :bytes
                                                                     :upload-data (-> fr .-result)
                                                                     :params {:source source}
                                                      }]))
      (.readAsArrayBuffer fr file))))

(rf/reg-event-fx
 :app/session.start
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [db local-storage]} [_]]
   (let [token (:token local-storage)]
     (if (seq token)
       {:ws-request {:name :users/session
                     :params {:token token}
                     :success-fn #(rf/dispatch [:ventas/db [:session] %])}}
       {}))))

(rf/reg-event-fx
 :app/session.stop
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [local-storage db]}]
   {:db (dissoc db :session)
    :local-storage (dissoc local-storage :token)}))

(rf/reg-event-fx :admin.users/edit
  (fn event-users-edit [cofx [_ data]]
    {:go-to [:admin.users.edit data]}))

(rf/reg-event-db :admin.users.edit/comments.edit
  (fn [db [_ id key-vec modal-key]]
    (-> db
        (assoc modal-key {:data (first (filter #(= (:id %) id) (get-in db key-vec)))
                               :open true}))))

(rf/reg-event-db :admin.users.edit/comments.modal
  (fn event-users-edit-comments-modal [db [_ k data]]
    (assoc db k data)))

(rf/reg-event-fx :admin.users.edit/comments.modal.submit
  (fn [fx [_ key-vec comm]]
    {:ws-request {:name :comments.save :params comm :success-fn #(rf/dispatch [:app/entity-update.next key-vec %])}}))

(defn page []
  (info "Rendering...")
  (rf/dispatch [:app/session.start])
  (let [current-page (:current-page (session/get :route))
        route-params (:route-params (session/get :route))]
    [p/pages current-page]))

(defn init []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (info "Current path" path)
      (let [match (routes/match-route path)
            current-page (:handler match)
            route-params (:route-params match)]
        (info "Current page" current-page)
        (session/put! :route {:current-page current-page
                              :route-params route-params})))
    :path-exists?
    (fn [path]
      (boolean (routes/match-route path)))})
  (go
    (when (<! (ws/init))
      (accountant/dispatch-current!)
      (reagent/render [page] (js/document.getElementById "app")))))

(defn start []
  (info "Starting ventas")
  (init))

(defn stop []
  (info "Stopping ventas"))

(defn on-figwheel-reload []
  (debug "Reloading...")
  (when-let [el (js/document.getElementById "app")]
    (reagent/render [page] el)))
