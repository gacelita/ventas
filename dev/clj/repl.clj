(ns repl
  (:require
   [clojure.tools.namespace.repl :as tn]
   [ventas-devtools.repl :as devtools.repl]
   [clojure.repl :refer :all]
   [clojure.core.async :as core.async :refer [<! chan >! go]]
   [ventas.themes.dev.core]
   [ventas.themes.devcards.core]
   [ventas.server]
   [clojure.spec.alpha :as spec]
   [ventas.server.spa]))

(def cljs-repl        devtools.repl/cljs-repl)
(def r                devtools.repl/r)
(def run-tests        devtools.repl/run-tests)
(def set-themes!      devtools.repl/set-themes!)
(def tn-refresh       tn/refresh)

(defn init []
  (devtools.repl/init)
  (set-themes! #{:dev :devcards :admin}))

