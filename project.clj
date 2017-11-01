(defproject ventas "0.0.2"
  :description "The Ventas eCommerce platform"
  :url "https://kazer.es"
  :license {:name "GPL"
            :url "https://opensource.org/licenses/GPL-2.0"}

  :repositories {"my.datomic.com"
                 {:url "https://my.datomic.com/repo"}}

  :dependencies [
                 ;; Clojure
                 [org.clojure/clojure "1.9.0-alpha19"]
                 [org.clojure/clojurescript "1.9.854" :scope "provided"]
                 [org.clojure/core.async "0.3.443"
                  :exclusions [org.clojure/tools.reader]]
                 [expound "0.2.1"]
                 [org.clojure/spec.alpha "0.1.123" :scope "provided"]

                 ;; Namespace tools
                 [org.clojure/tools.namespace "0.3.0-alpha4"]

                 ;; Conflict resolution
                 [com.google.guava/guava "23.0"]

                 ;; Logging
                 [org.clojure/tools.logging "0.4.0"]
                 [com.taoensso/timbre       "4.10.0"]
                 [onelog "0.5.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]

                 ;; JSON, Transit and Fressian
                 [org.clojure/data.json "0.2.6"]
                 [cheshire "5.8.0"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [org.clojure/data.fressian "0.2.1"]

                 ;; Server-side HTTP requests
                 [clj-http "3.7.0" :exclusions [riddley]]

                 ; Server
                 [http-kit "2.2.0"]

                 ; Authentication
                 [buddy "2.0.0" :exclusions [instaparse]]

                 ;; Ring
                 [ring "1.6.2"]
                 [ring/ring-defaults "0.3.1"]
                 [bk/ring-gzip "0.2.1"]
                 [ring.middleware.logger "0.5.0" :exclusions [log4j onelog]]
                 [ring/ring-json "0.4.0" :exclusions [cheshire]] ;; see: buddy

                 ;; Routing
                 [compojure "1.6.0" :exclusions [instaparse]]

                 ;; Configuration
                 [cprop "0.1.11"]

                 ;; i18n
                 [tongue "0.2.2"]

                 ;; Reagent
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [re-frame "0.10.1"]
                 [re-frame-datatable "0.6.0"]
                 [soda-ash "0.4.0"]

                 ; Routing
                 [bidi "2.1.2"]
                 [venantius/accountant "0.2.0"]

                 ; HTML templating
                 [selmer "1.11.1" :exclusions [cheshire joda-time]]

                 ;; component alternative
                 [mount "0.1.11"]

                 ;; Process starting and stopping
                 [me.raynes/conch "0.8.0"]

                 ;; Database
                 [com.datomic/datomic-pro "0.9.5394" :exclusions [org.slf4j/log4j-over-slf4j org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]
                 [io.rkn/conformity "0.5.1"]

                 ;; Text colors
                 [io.aviso/pretty "0.1.34"]

                 ;; UUIDs
                 [danlentz/clj-uuid "0.1.7" :exclusions [primitive-math]]

                 ;; "throw+" and "try+"
                 [slingshot "0.12.2"]

                 ;; Uploads
                 [byte-streams "0.2.3"]
                 [com.novemberain/pantomime "2.9.0"]

                 ;; DateTime
                 [clj-time "0.14.0"]

                 ;; localStorage
                 [alandipert/storage-atom "2.0.1"]

                 ;; Generators
                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.8" :exclusions [instaparse]]

                 [binaryage/devtools "0.9.4"]

                 ; Error reporting for Ring
                 [prone "1.1.4"]
                 [devcards "0.2.3" :exclusions [cljsjs/react]]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-sassc "0.10.4" :exclusions [org.apache.commons/commons-compress org.clojure/clojure]]
            [lein-auto "0.1.3"]
            [lein-ancient "0.6.10"]
            [venantius/ultra "0.5.1" :exclusions [org.clojure/clojure]]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs" "src/cljc" "custom-lib"]

  :test-paths ["test/clj" "test/cljc"]

  :jvm-opts ["-Xverify:none" "-XX:-OmitStackTraceInFastThrow"]

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/files/js"]

  :uberjar-name "ventas.jar"

  :main ventas.server

  :repl-options {:init-ns user :port 4001 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :aliases {"config" ["run" "-m" "outpace.config.generate"]}

  :cljsbuild {:builds
              [{:id "app"
                :source-paths ["src/cljs" "src/cljc" "test/cljs" "test/cljc" "custom-lib"]

                :figwheel {:on-jsload "ventas.core/on-figwheel-reload"}

                :compiler {:main ventas.core
                           :asset-path "files/js/compiled/out"
                           :closure-defines {"clairvoyant.core.devmode" true}
                           :output-to "resources/public/files/js/compiled/ventas.js"
                           :output-dir "resources/public/files/js/compiled/out"
                           :source-map-timestamp true
                           :devcards true
                           :preloads [devtools.preload]
                           :parallel-build true}}

               {:id "test"
                :source-paths ["src/cljs" "src/cljc" "test/cljs" "test/cljc" "test/doo" "custom-lib"]
                :compiler {:output-to "resources/public/files/js/compiled/testable.js"
                           :main ventas.test-runner
                           :optimizations :none
                           :parallel-build true}}

               {:id "min"
                :source-paths ["src/cljs" "src/cljc" "custom-lib"]
                :jar true
                :compiler {:main ventas.core
                           :output-to "resources/public/files/js/compiled/ventas.js"
                           :output-dir "target"
                           :source-map-timestamp true
                           :optimizations :advanced
                           :pretty-print false
                           :externs ["externs.js"]
                           :parallel-build true}}]}

  :figwheel {:css-dirs ["resources/public/files/css"]
             :open-file-command "open-with-subl3"
             :server-logfile "log/figwheel.log"
             :repl false}

  :doo {:build "test"}

  :sassc [{:src "src/scss/main.scss"
           :output-to "resources/public/files/css/style.css"
           :style "nested"
           :import-path "src/scss"}]

  :auto {"sassc" {:file-pattern  #"\.(scss)$"
                  :paths ["src/scss"]}}

  :profiles {:dev {:dependencies [[figwheel "0.5.13"]
                                  [figwheel-sidecar "0.5.13"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [com.cemerick/pomegranate "0.4.0" :exclusions [org.codehaus.plexus/plexus-utils]]
                                  [org.clojure/test.check "0.9.0"]
                                  [com.gfredericks/test.chuck "0.2.8"]]
                    :plugins [[lein-figwheel "0.5.13" :exclusions [org.clojure/clojure]]
                              [lein-doo "0.1.7" :exclusions [org.clojure/clojure]]]
                    :source-paths ["dev"]}

              :uberjar {:source-paths ^:replace ["src/clj" "src/cljc" "custom-lib"]
                        :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                        :hooks [leiningen.sassc]
                        :omit-source true
                        :aot :all}})