(ns ventas.core
  "Try to keep this namespace on a strict diet :)"
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :refer [<! go]]
   [cljs.pprint :as pprint]
   [cognitect.transit :as transit]
   [re-frame.core :as rf]
   [re-frame.loggers :as rf.loggers]
   [reagent.core :as reagent]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend]
   [ventas.local-storage :as storage]
   [ventas.page :as page]
   [ventas.pages.admin]
   [ventas.routes :as routes]
   [ventas.seo :as seo]
   [ventas.session :as session]
   [ventas.utils.logging :refer [debug info]]
   [ventas.ws :as ws]))

(enable-console-print!)

;; Dear re-frame: no one cares about handlers being overwritten
;; Thank you
;; (Would love to see this moved to verbose logging upstream some day)
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

(rf/reg-fx
 :document-title
 (fn [title]
   (set! js/document.title title)))

(rf/reg-fx
 :redirect
 (fn [url]
   (set! js/document.location url)))

(def ^:private id->interval (atom {}))

(rf/reg-fx
 :set-interval
 (fn [{:keys [speed callback id]}]
   (swap! id->interval assoc id (js/setInterval callback speed))))

(rf/reg-fx
 :clear-interval
 (fn [id]
   (js/clearInterval (get @id->interval id))
   (swap! id->interval dissoc id)))

(rf/reg-fx
 :aset
 (fn [args]
   (apply aset args)))

(rf/reg-event-fx
 ::rendered-db
 (fn [_ _]
   (when-let [rendered (aget js/window "__rendered_db")]
     (let [reader (transit/reader :json)]
       {:db (transit/read reader rendered)
        :aset [js/window "__rendered_db" nil]}))))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::rendered-db]
                 [::events/users.session]]}))

(defn- page []
  (info "Rendering...")
  [page/main (routes/handler)])

(defn- error-view [{:keys [message] :as error}]
  [:div#main
   [:div.root
    [:div.centered-segment-wrapper
     [:div.centered-segment
      [base/segment {:color "red"}
       (if message
         [:p message]
         [:pre (with-out-str (pprint/pprint error))])]]]]])

(defn app-element []
  (js/document.getElementById "app"))

(defn nav-handler [path]
  (info "Current path" path)
  (let [{:keys [handler route-params]} (routes/match-route path)]
    (info "Current page" handler)
    (rf/dispatch [::routes/set handler route-params])))

(defn init []
  (.addEventListener js/window
                     "resize"
                     #(rf/dispatch [::events/db [:window] {:width js/window.innerWidth
                                                           :height js/window.innerHeight}]))
  (accountant/configure-navigation!
   {:nav-handler #'nav-handler
    :path-exists?
    (fn [path]
      (boolean (routes/match-route path)))})
  (go
   (when-not (seo/rendered?)
     (reagent/render [base/loading] (app-element)))

   (debug "Init stage 1 - Starting websockets")
   (when-not (<! (ws/init))
     (throw (js/Error. "Websocket initialization problem")))

   (debug "Init stage 2- Syncing rendered db, starting WS session")
   (rf/dispatch-sync [::init])

   (let [{:keys [success message]} (<! session/ready)]
     (if-not success
       (do
         (js/console.error (str "Session initialization problem\n"
                                (with-out-str (pprint/pprint message))))
         (reagent/render [error-view message] (app-element)))
       (do
         (debug "Init stage 3 - Rendering")
         (accountant/dispatch-current!)
         (reagent/render [page] (app-element)))))))

(defn ^:export start []
  (info "Starting...")
  (init))

(defn on-reload []
  (debug "Reloading...")
  (when-let [element (app-element)]
    (reagent/render [page] element)))
