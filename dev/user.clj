(ns user
  "Configuration"
  (:require
   [figwheel-sidecar.repl-api :as figwheel]
   [clojure.tools.namespace.repl :as tn]
   [clojure.stacktrace :as st :refer [print-stack-trace]]
   [clojure.repl :refer :all]
   [clojure.core.async :refer [>! go]]
   [repl]))

;; Let Clojure warn you when it needs to reflect on types.
;; Type annotations should be added in such cases to prevent degraded performance.
(alter-var-root #'*warn-on-reflection* (constantly true))

(clojure.tools.namespace.repl/set-refresh-dirs "src/clj" "src/cljc" "dev" "test/clj" "test/cljc")

;; CLJS REPL
(def cljs-repl figwheel/cljs-repl)

(def init repl/init)