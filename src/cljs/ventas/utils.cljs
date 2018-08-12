(ns ventas.utils
  "Random utilities"
  (:require
   [cljs.spec.alpha :as spec]
   [expound.alpha :as expound]
   [ventas.utils.logging :refer [debug error info trace warn]]
   [clojure.string :as str])
  (:require-macros
   [ventas.utils]))

(defn value-handler [callback]
  (fn [e]
    (callback (-> e .-target .-value))))

(defn child? [element target]
  (when-let [parent (.-parentNode element)]
    (if (= target parent)
      true
      (child? parent target))))

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

(defn check [& args]
  "([spec x] [spec x form])
     Returns true when x is valid for spec. Throws an Error if validation fails."
  (if (apply spec/valid? args)
    true
    (throw (js/Error. (with-out-str (apply expound/expound args))))))

(defn debounce
  "Returns a function that will call f only after threshold has passed without new calls
  to the function. Calls prep-fn on the args in a sync way, which can be used for things like
  calling .persist on the event object to be able to access the event attributes in f"
  ([threshold f] (debounce threshold f (constantly nil)))
  ([threshold f prep-fn]
   (let [t (atom nil)]
     (fn [& args]
       (when @t (js/clearTimeout @t))
       (apply prep-fn args)
       (reset! t (js/setTimeout #(do
                                   (reset! t nil)
                                   (apply f args))
                                threshold))))))

(defn parse-int [n]
  (js/parseInt n 10))

(defn render-with-indexes [& coll]
  (map-indexed (fn [idx itm]
                 (with-meta itm {:key idx}))
               coll))

(defn cut-string-on-space [s max-chars]
  (when s
    (let [last-idx (.lastIndexOf (take max-chars s) \space)
          idx (if (pos? last-idx)
                last-idx
                max-chars)]
      (subs s 0 idx))))