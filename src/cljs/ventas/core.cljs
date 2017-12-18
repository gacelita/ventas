(ns ventas.core
  "Try to keep this namespace on a strict diet :)"
  (:require
   [reagent.core :as reagent]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [re-frame.loggers :as rf.loggers]
   [accountant.core :as accountant]
   [ventas.utils.logging :refer [debug info]]
   [cljs.core.async :refer [<!]]
   [ventas.events.backend :as backend]
   [ventas.ws :as ws]
   [ventas.local-storage :as storage]
   [ventas.devcards.core]
   [ventas.routes :as routes]
   [ventas.page :as p]
   [ventas.plugins.core]
   [ventas.pages.admin]
   [ventas.pages.datadmin]
   [ventas.pages.api]
   [ventas.themes.clothing.core]
   [ventas.components.base :as base]
   [ventas.events :as events])
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

(defn loading []
  [base/segment
   [base/dimmer {:active true
                 :inverted true}
    [base/loader {:inverted true}
     "Loading"]]])

(defn page []
  (info "Rendering...")
  (rf/dispatch [::events/session.start])
  (let [session @(rf/subscribe [::events/db [:session]])]
    (if-not session
      [loading]
      (let [{:keys [current-page]} (session/get :route)]
        [p/pages current-page]))))

(defn app-element []
  (js/document.getElementById "app"))

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
      (reagent/render [page] (app-element)))))

(defn ^:export start []
  (info "Starting...")
  (init))

(defn on-figwheel-reload []
  (debug "Reloading...")
  (when-let [element (app-element)]
    (reagent/render [page] element)))
