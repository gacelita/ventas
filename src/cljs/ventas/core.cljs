(ns ventas.core
  "Try to keep this namespace on a strict diet :)"
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :refer [<!]]
   [re-frame.core :as rf]
   [re-frame.loggers :as rf.loggers]
   [reagent.core :as reagent]
   [ventas.components.base :as base]
   [ventas.devcards.core]
   [ventas.events :as events]
   [ventas.events.backend]
   [ventas.local-storage :as storage]
   [ventas.page :as page]
   [ventas.pages.admin]
   [ventas.pages.datadmin]
   [ventas.plugins.api.core]
   [ventas.routes :as routes]
   [ventas.seo :as seo]
   [ventas.utils.logging :refer [debug info]]
   [ventas.ws :as ws]
   [cljs.reader :as reader]
   [ventas.utils.logging :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

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
 :aset
 (fn [args]
   (log/debug :aset args)
   (apply aset args)))

(rf/reg-event-fx
 ::rendered-db
 (fn [{:keys [db]} _]
   (when-let [rendered (aget js/window "__rendered_db")]
     {:db (reader/read-string rendered)
      :aset [js/window "__rendered_db" nil]})))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {:dispatch-n [[::rendered-db]
                 [::events/users.session]
                 [::events/categories.list]
                 [::events/users.favorites.enumerate]]}))

(defn page []
  (info "Rendering...")
  (rf/dispatch [::init])
  (let [session @(rf/subscribe [::events/db [:session]])]
    (if (seo/rendered?)
      [page/main (routes/handler)]
      (if-not session
        [base/loading]
        [page/main (routes/handler)]))))

(defn app-element []
  (js/document.getElementById "app"))

(defn init []
  (.addEventListener js/window
                     "resize"
                     #(rf/dispatch [::events/db [:window] {:width js/window.innerWidth
                                                           :height js/window.innerHeight}]))
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (info "Current path" path)
      (let [{:keys [handler route-params]} (routes/match-route path)]
        (info "Current page" handler)
        (rf/dispatch [::routes/set handler route-params])))
    :path-exists?
    (fn [path]
      (boolean (routes/match-route path)))})
  (go
    (when (<! (ws/init))
      (accountant/dispatch-current!)
      (reagent/render [page] (app-element)))))

(defn ^:export start []
  (info "Starting...")
  (init))

(defn on-figwheel-reload []
  (debug "Reloading...")
  (when-let [element (app-element)]
    (reagent/render [page] element)))
