(ns ventas.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [accountant.core :as accountant]
            [cljsjs.react-bootstrap]
            [clojure.string :as s]
            [taoensso.timbre :as timbre :refer-macros [tracef debugf infof warnf errorf
                                                       trace debug info warn error]]
            [cljs.core.async :refer [<! >! put! close! timeout chan]]
            [chord.client :refer [ws-ch]]
            [chord.format.fressian :as chord-fressian]
            [cljs.reader :as edn]
            [cljs.pprint :as pprint]
            
            [ventas.ws :as ws]
            [ventas.subs :as subs]
            [ventas.util :as util :refer [dispatch-page-event]]
            [ventas.local-storage :as storage]

            [re-frame-datatable.core :as dt]
            [soda-ash.core :as sa]

            ;; Tracing
            [clairvoyant.core :refer-macros [trace-forms]]
            [re-frame-tracer.core :refer [tracer]]

            [ventas.devcards.core]

            [ventas.routes :as routes :refer [go-to]]
            [ventas.page :as p]

            [ventas.plugins.featured-products.core]
            [ventas.pages.backend]
            [ventas.pages.backend.playground]
            [ventas.pages.backend.users]
            [ventas.pages.backend.users.edit]
            [ventas.pages.datadmin]
            [ventas.pages.api]
            [ventas.themes.mariscosriasbajas.core]
            )
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)]
    [ventas.util-macros :as util-macros :refer [require-pages require-plugins]]))

(enable-console-print!)
(timbre/set-level! :trace)

(storage/reg-co-fx!
 :ventas
 {:fx :local-storage
  :cofx :local-storage})

;; (require-pages)
;; (require-plugins)


(defmulti page-start (fn [page cofx] page))

(defmethod page-start :default [page cofx]
  {})

(defmethod page-start :backend.users [page cofx]
  {:ws-request {:name :users.list :success-fn #(rf/dispatch [:app/entity-query.next [:users] %])}})

(defmethod page-start :backend.users.edit [page cofx]
  (let [id (js/parseInt (get-in (session/get :route) [:route-params :id]))]
    (if (> id 0)
      {:ws-request-multi [
                          {:name :entities.find :params {:id id} :success-fn #(rf/dispatch [:app/entity-query.next [:form] %])}
                          {:name :users.comments.list :params {:id id} :success-fn #(rf/dispatch [:app/entity-query.next [:form :comments] %])}
                          {:name :users.images.list :params {:id id} :success-fn #(rf/dispatch [:app/entity-query.next [:form :images] %])}
                          {:name :users.own-images.list :params {:id id} :success-fn #(rf/dispatch [:app/entity-query.next [:form :own-images] %])}
                          {:name :users.friends.list :params {:id id} :success-fn #(rf/dispatch [:app/entity-query.next [:form :friends] %])}
                          {:name :users.made-comments.list :params {:id id} :success-fn #(rf/dispatch [:app/entity-query.next [:form :made-comments] %])}
                          {:name :backend.reference/user.role :success-fn #(rf/dispatch [:app/entity-query.next [:reference :user.role] %])}
                          ]}
      {:db (assoc (:db cofx) :form {:name "" :password "" :email "" :description "" :roles []})})))

(defmulti page-end (fn [page cofx] page))

(defmethod page-end :backend.users.edit [_ cofx]
  {:db (assoc (:db cofx) :form {})})

(defmethod page-end :default [_ _]
  (debug "No end function")
  {})


;; Effects

(defn effect-ws-request [request]
  (ws/send-request!
   {:name (:name request)
    :params (:params request)
    :request-params (:request-params request)
    :callback (fn [data] (cond
                           (not (:success data))
                             (rf/dispatch [:app/notifications.add {:message (:data data) :theme "warning"}])
                           (:success request)
                             (rf/dispatch [(:success request) (:data data)])
                           (:success-fn request)
                             ((:success-fn request) (:data data))))}))

(def ws-upload-chunk-size (* 1024 50))
(defn effect-ws-upload-request
  ([request] (effect-ws-upload-request request false 0))
  ([request file-id start]
    (let [data (:upload-data request)
          data-length (-> data .-byteLength)
          raw-end (+ start ws-upload-chunk-size)
          is-last (> raw-end data-length)
          is-first (zero? start)
          end (if is-last data-length raw-end)
          chunk (.slice data start end)]
      (ws/send-request!
        {:name (:name request)
         :params (-> (:params request) (assoc (:upload-key request) chunk) (assoc :is-last is-last) (assoc :is-first is-first) (assoc :file-id file-id))
         :request-params {:binary true :chunked true}
         :callback  (fn [response]
                      (debug "Executing upload callback" start end is-first is-last)
                      (if-not is-last
                        (effect-ws-upload-request request (if is-first (:data response) file-id) end)
                        (fn [a] ((:success-fn request) (:data response)))))}))))
    

(rf/reg-fx :ws-request effect-ws-request)
(rf/reg-fx :ws-request-multi (fn [requests] (doseq [request requests] (effect-ws-request request))))
(rf/reg-event-fx :effects/ws-request
  (fn [cofx [_ data]]
    (debug ":effects/ws-request")
    {:ws-request data}))
(rf/reg-fx :ws-upload-request effect-ws-upload-request)
(rf/reg-event-fx :effects/ws-upload-request
  (fn [cofx [_ data]]
    (debug ":effects/ws-upload-request")
    {:ws-upload-request data}))

(rf/reg-fx :go-to
  (fn effect-go-to [data]
    (go-to (get data 0) (get data 1))))


;; Events

;; TODO: Upload, subscription, CLJS modules, autocomplete

(rf/reg-event-db :app/entity-update.next
  (fn [db [_ where what]]
    (debug "entity-update, where: " where)
    (assoc-in db where (map #(if (= (:id %1) (:id what)) what %1) (get-in db where)))))

(rf/reg-event-db :app/entity-query.next
  (fn [db [_ where what]]
    (debug "entity-query, where: " where)
    (assoc-in db where what)))

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
                     :success-fn #(rf/dispatch [:app/entity-query.next [:session] %])}}
       {}))))

(rf/reg-event-fx
 :app/session.stop
 [(rf/inject-cofx :local-storage)]
 (fn [{:keys [local-storage db]}]
   {:db (dissoc db :session)
    :local-storage (dissoc local-storage :token)}))

(rf/reg-event-fx :backend.users/edit
  (fn event-users-edit [cofx [_ data]]
    {:go-to [:backend.users.edit data]}))

(rf/reg-event-fx :backend.users.edit/submit
  (fn event-users-edit-submit [cofx [_ data]]
    {:ws-request {:name :users.save :params data :success :backend.users.edit/submit.next}}))

(rf/reg-event-fx :backend.users.edit/submit.next
  (fn event-users-edit-submit-next [cofx [_ data]]
    (debug "About to dispatch go-to app.users")
    {:dispatch [:app/notifications.add {:message "Usuario guardado satisfactoriamente" :theme "success"}]
     :go-to [:backend.users]}))

(rf/reg-event-db :backend.users.edit/comments.edit
  (fn [db [_ id key-vec modal-key]]
    (-> db
        (assoc modal-key {:data (first (filter #(= (:id %) id) (get-in db key-vec)))
                               :open true}))))

(rf/reg-event-db :backend.users.edit/comments.modal
  (fn event-users-edit-comments-modal [db [_ k data]]
    (assoc db k data)))

(rf/reg-event-fx :backend.users.edit/comments.modal.submit
  (fn [fx [_ key-vec comm]]
    {:ws-request {:name :comments.save :params comm :success-fn #(rf/dispatch [:app/entity-update.next key-vec %])}}))

(rf/reg-event-fx :navigation-start
  (fn event-navigation-start [cofx [_ new-page old-page]]
    (debug "event-navigation-start" new-page old-page)
    (let [start (page-start new-page cofx)
          end (page-end old-page cofx)]
      (merge start end))))

(rf/reg-event-db :initialize
  (fn event-initialize [_ _]
    {:users {} :messages [] :session {}}))

(rf/reg-event-db :users-initialize
  (fn event-users-initialize [db [_ users]]
    (assoc db :users users)))

(rf/reg-event-db :session-initialize
  (fn event-session-initialize [db [_ session]]
    (assoc db :session session)))

(rf/reg-event-db :users-add
  (fn event-users-add [db [_ user]]
    (assoc-in db [:users (:id user)] user)))

(rf/reg-event-db :users-replace
  (fn event-users-replace [db [_ users]]
    (assoc db :users users)))

(rf/reg-event-db :messages-add
  (fn event-messages-add [db [_ message]]
    (debug ":messages-add" message)
    (let [messages (take 10 (conj (:messages db) (cond
                                                   (nil? message) {:type :connection-closed}
                                                   :else message)))]
      (debug "messages:" messages)
      (assoc-in db [:messages] messages))))

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
        (info "Current page: " current-page)
        (rf/dispatch [:navigation-start current-page (:current-page (session/get :route))])
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
  (infof "Starting ventas")
  (init))

(defn stop []
  (infof "Stopping ventas"))

(defn on-figwheel-reload []
  (debug "Reloading...")
  (when-let [el (js/document.getElementById "app")]
    (reagent/render [page] el)))
