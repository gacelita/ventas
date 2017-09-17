(ns ventas.util
  (:require [bidi.bidi :as bidi]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [ventas.routes :as routes]
            [ventas.utils.logging :refer [trace debug info warn error]]))

(defn route-param [kw]
  (get-in (session/get :route) [:route-params kw]))

(defn dispatch-page-event [data]
  (debug "Dispatching: " (keyword (:current-page (session/get :route)) (name (get data 0))))
  (rf/dispatch (assoc data 0 (keyword (:current-page (session/get :route)) (name (get data 0))))))

(defn gen-key []
  (gensym "key-"))

(defn value-handler [callback]
  (fn [e]
    (callback (-> e .-target .-value))))

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

(defn sub-resource-url [resourceId]
  (debug "get-resource-url")
  (let [sub-kw (keyword "resources" (str resourceId))]
    (rf/reg-sub sub-kw
                (fn [db _] (-> db sub-kw)))
    (rf/dispatch [:effects/ws-request {:name :resource/get
                                       :params {:keyword resourceId}
                                       :success-fn #(rf/dispatch [:ventas.api/success [sub-kw] %])}])
    sub-kw))

(defn sub-configuration [kw]
  (debug "get-configuration")
  (let [sub-kw (keyword "configuration" (str kw))]
    (rf/reg-sub sub-kw
      (fn [db _] (-> db sub-kw)))
    (rf/dispatch [:effects/ws-request {:name :configuration/get
                                       :params {:key kw}
                                       :success-fn #(rf/dispatch [:ventas.api/success [sub-kw] %])}])
    sub-kw))

(defn format-price
  "Really naive method"
  [price]
  (str price " €"))