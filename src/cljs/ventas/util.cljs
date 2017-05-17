(ns ventas.util
  (:require [bidi.bidi :as bidi]
            [accountant.core :as accountant]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [ventas.routes :refer [route-parents routes raw-route]]
            [taoensso.timbre :as timbre :refer-macros [tracef debugf infof warnf errorf
                                                       trace debug info warn error]]))

(defn go-to [routes route route-params]
  (debug "Going to" route route-params)
  (accountant/navigate! (apply bidi/path-for (concat [routes route] (first (seq route-params))))))

(defn route-param [kw]
  (get-in (session/get :route) [:route-params kw]))

(defn dispatch-page-event [data]
  (debug "Dispatching: " (keyword (:current-page (session/get :route)) (name (get data 0))))
  (rf/dispatch (assoc data 0 (keyword (:current-page (session/get :route)) (name (get data 0))))))

(defn gen-key []
  (gensym "key-"))

(defn interpose-fn
  "Returns a lazy seq of the elements of coll separated by sep.
  Returns a stateful transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([sep]
   (fn [rf]
     (let [started (volatile! false)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (if @started
            (let [sepr (rf result sep)]
              (if (reduced? sepr)
                sepr
                (rf sepr input)))
            (do
              (vreset! started true)
              (rf result input))))))))
  ([sep coll]
   (drop 1 (interleave (repeatedly sep) coll))))

(defn wrap-with-model [data]
  "Wraps a component with a model binding"
    (-> data (assoc :default-value (get @(:model data) (keyword (:name data))))
             (assoc :on-change #(swap! (:model data) assoc (keyword (:name data)) (do (debug "setting " (-> % .-target .-value)) (-> % .-target .-value))))))

(defn wrap-sa-with-model [data]
  "Wraps a Soda-Ash component with a model binding"
  (-> data (assoc :default-value (get @(:model data) (keyword (:name data))))
             (assoc :on-change (fn [e field-data]
                                  (js/console.log "field-data: " field-data)
                                  (swap! (:model data) assoc (keyword (:name data))
                                      (do (debug "setting " (-> field-data .-value)) (js->clj (-> field-data .-value))))
                                  (js/console.log "Value now: " @(:model data))))))

(defn breadcrumbs [current-route route-params]
  (map (fn [route] {:url (apply bidi/path-for (concat [routes route] (first (seq route-params))))
                    :name (:name (raw-route route))
                    :route route}) (route-parents current-route)))