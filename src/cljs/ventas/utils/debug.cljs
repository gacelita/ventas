(ns ventas.utils.debug
  (:require
   [cljs.pprint :as pprint]))

(defn pprint-sub [sub]
  [:pre (with-out-str (pprint/pprint @sub))])