(ns ventas.seo
  "Prerendering stuff"
  (:require
   [cljs.pprint :as pprint]
   [cljs.reader :as reader]
   [re-frame.core :as rf]
   [ventas.events :as events]
   [ventas.routes :as routes]
   [ventas.ws :as ws]
   [cognitect.transit :as transit]))

(defn ^:export go-to [args]
  (apply routes/go-to
         (reader/read-string args)))

(defn rendered? []
  (js/document.querySelector "#app > #main"))

(defn ^:export ready? []
  (and
   (not (ws/requests-pending?))
   (rendered?)
   (= js/document.readyState "complete")))

(defn ^:export dump-db []
  (let [writer (transit/writer :json)]
    (transit/write writer @(re-frame.core/subscribe [::events/db]))))

(defonce ^:private prerendering-hooks (atom {}))

(defn add-prerendering-hook [id f]
  (swap! prerendering-hooks assoc id f))

(rf/reg-event-db
 ::execute-prerendering-hooks
 (fn [db _]
   (reduce (fn [db f]
             (f db))
           db
           (vals @prerendering-hooks))))

(defn ^:export execute-prerendering-hooks []
  (rf/dispatch-sync [::execute-prerendering-hooks]))
