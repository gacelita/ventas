(ns ventas.utils
  (:require
   [bidi.bidi :as bidi]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [ventas.routes :as routes]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.utils.formatting :as utils.formatting]
   [cljs.spec.alpha :as spec]
   [expound.alpha :as expound]))

(defn route-param [kw]
  (get-in (session/get :route) [:route-params kw]))

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

(defn format-price
  "Really naive method"
  [price]
  (utils.formatting/format-number price ::utils.formatting/euro))

(defn check [& args]
  "([spec x] [spec x form])
     Returns true when x is valid for spec. Throws an Error if validation fails."
  (if (apply spec/valid? args)
    true
    (throw (js/Error. (with-out-str (apply expound/expound args))))))