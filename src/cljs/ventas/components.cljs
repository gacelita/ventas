(ns ventas.components
  (:require [ventas.util :as util]
            [soda-ash.core :as sa]))


(defn input-with-model [data]
  "An input with a model binding"
  [sa/FormInput (util/wrap-with-model data)])

(defn textarea-with-model [data]
  "A textarea with a model binding"
  [sa/FormTextArea (util/wrap-with-model data)])