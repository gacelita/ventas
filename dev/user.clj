(ns user
  "Development env configuration.
   Don't add ventas.* aliases in this ns, as it conflicts with dynamic aliases
   and nonstrict classloading."
  (:require
   [figwheel-sidecar.repl-api :as figwheel]
   ;; kept for REPL usage
   [clojure.tools.namespace.repl :as tn]
   [clojure.repl :refer :all]
   [clojure.core.async :refer [>! go]]
   [repl]
   [ventas.config]))

;; Let Clojure warn you when it needs to reflect on types.
;; Type annotations should be added in such cases to prevent degraded performance.
(alter-var-root #'*warn-on-reflection* (constantly true))

(cond->> ["src/clj" "src/cljc" "test/clj" "test/cljc"]
  (not (ventas.config/get :strict-classloading)) (concat ["dev"])
  true (apply clojure.tools.namespace.repl/set-refresh-dirs))

;; CLJS REPL
(def cljs-repl figwheel/cljs-repl)

(def init repl/init)
