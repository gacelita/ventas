(ns ventas.core
  (:require
   [reagent.core :as reagent]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [re-frame.loggers :as rf.loggers]
   [accountant.core :as accountant]
   [ventas.utils.logging :refer [debug info]]
   [cljs.core.async :refer [<!]]
   [ventas.api :as api]
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
   [ventas.components.base :as base])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

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

(rf/reg-fx :go-to
  (fn [[route params]]
    (routes/go-to route params)))

(defn loading []
  [base/segment
   [base/dimmer {:active true
                 :inverted true}
    [base/loader {:inverted true}
     "Loading"]]])

(defn page []
  (info "Rendering...")
  (rf/dispatch [:ventas/session.start])
  (let [session @(rf/subscribe [:ventas/db [:session]])]
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

(defn start []
  (info "Starting...")
  (init))

(defn on-figwheel-reload []
  (debug "Reloading...")
  (when-let [element (app-element)]
    (reagent/render [page] element)))
