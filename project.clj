(defproject ventas "0.0.1"
  :description "The Ventas eCommerce platform"
  :url "https://kazer.es"
  :license {:name "GPL"
            :url "https://opensource.org/licenses/GPL-2.0"}

  :repositories {"my.datomic.com"
                 {:url "https://my.datomic.com/repo"}}

  :dependencies [
                 ;; Clojure
                 [org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [org.clojure/core.async "0.2.395"
                  :exclusions [org.clojure/tools.reader]]

                 ;; Namespace tools
                 [org.clojure/tools.namespace "0.2.11"]

                 ;; Logging
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]

                 ;; JSON, Transit and Fressian
                 [org.clojure/data.json "0.2.6"]
                 [cheshire "5.5.0"]
                 [com.cognitect/transit-clj "0.8.297"]
                 [com.cognitect/transit-cljs "0.8.220"]
                 [org.clojure/data.fressian "0.2.1"]

                 ;; Server-side HTTP requests
                 [clj-http "2.3.0"]

                 ;; Logging
                 [com.taoensso/timbre       "4.8.0"]

                 ;; core.async + websockets
                 ;; [jarohen/chord "0.7.0"]

                 ; Server
                 [http-kit "2.2.0"]

                 ; Authentication
                 [buddy "1.2.0"]

                 ;; [longshi "0.1.4"]
                 ; [net.unit8/fressian-cljs "0.2.0"]

                 ;; Ring
                 [ring "1.5.1"]
                 [ring/ring-defaults "0.2.2"]
                 [bk/ring-gzip "0.2.1"]
                 [onelog "0.4.6"]
                 [ring.middleware.logger "0.5.0" :exclusions [log4j onelog]]
                 [ring/ring-json "0.4.0" :exclusions [cheshire]] ;; see: buddy

                 ;; Routing
                 [compojure "1.5.2"]

                 ;; Secrets
                 [cprop "0.1.10"]

                 ;; Reagent
                 [reagent "0.6.0"]
                 [reagent-utils "0.1.7"]
                 [re-frame "0.9.1"]
                 [re-frame-datatable "0.5.1"]
                 [soda-ash "0.2.0"]

                 ; Routing
                 [bidi "2.0.16"]
                 [venantius/accountant "0.1.7"]

                 ; HTML templating
                 [selmer "1.10.5" :exclusions [cheshire]] ;; see: buddy

                 ;; Bootstrap
                 [cljsjs/react-bootstrap "0.30.7-0"]

                 ;; component alternative
                 [mount "0.1.11"]

                 ;; Process starting and stopping
                 [me.raynes/conch "0.8.0"]

                 ;; Database
                 [com.datomic/datomic-pro "0.9.5394" :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]
                 [io.rkn/conformity "0.4.0"]

                 ;; Text colors
                 [io.aviso/pretty "0.1.33"]

                 ;; UUIDs
                 [danlentz/clj-uuid "0.1.6"]

                 ;; "throw+" and "try+"
                 [slingshot "0.12.2"]

                 ;; Uploads
                 [byte-streams "0.2.2"]
                 [com.novemberain/pantomime "2.9.0"]

                 ;; DateTime
                 [clj-time "0.13.0"]

                 ;; LocalStorage
                 [alandipert/storage-atom "2.0.1"]

                 ;; Generators
                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.7"]

                 ;;
                 ;; Debugging
                 ;;

                 ;; Google Chrome Developer Tools custom formatters
                 [binaryage/devtools "0.8.3"]

                 ; Error reporting for Ring
                 [prone "1.1.4"]

                 ;; re-frame debugging
                 [org.clojars.stumitchell/clairvoyant "0.2.0"]
                 [day8/re-frame-tracer "0.1.1-SNAPSHOT"]

                ]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-environ "1.0.3"]
            [lein-sassc "0.10.4"]
            [lein-auto "0.1.2"]
            [lein-ancient "0.6.10"]
            [lein-git-deps "0.0.1-SNAPSHOT"]
            [venantius/ultra "0.5.1"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs" "src/cljc" "custom-lib"]

  :test-paths ["test/clj" "test/cljc"]

  :jvm-opts ["-Xverify:none" "-XX:-OmitStackTraceInFastThrow"]

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]

  :uberjar-name "ventas.jar"

  ;; Use `lein run` if you just want to start a HTTP server, without figwheel
  :main ventas.server


  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (run) and
  ;; (browser-repl) live.
  :repl-options {:init-ns user :port 4001 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :aliases {"config" ["run" "-m" "outpace.config.generate"]}

  :cljsbuild {:builds
              [{:id "app"
                :source-paths ["src/cljs" "src/cljc" "custom-lib"]

                :figwheel {:on-jsload "ventas.core/on-figwheel-reload"}

                :compiler {:main ventas.core
                           :asset-path "js/compiled/out"
                           :closure-defines {"clairvoyant.core.devmode" true}
                           :output-to "resources/public/js/compiled/ventas.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}

               {:id "test"
                :source-paths ["src/cljs" "test/cljs" "src/cljc" "test/cljc" "custom-lib"]
                :compiler {:output-to "resources/public/js/compiled/testable.js"
                           :main ventas.test-runner
                           :optimizations :none}}

               {:id "min"
                :source-paths ["src/cljs" "src/cljc" "custom-lib"]
                :jar true
                :compiler {:main ventas.core
                           :output-to "resources/public/js/compiled/ventas.js"
                           :output-dir "target"
                           :source-map-timestamp true
                           :optimizations :advanced
                           :pretty-print false
                           :externs ["externs.js"]}}]}

  ;; When running figwheel from nREPL, figwheel will read this configuration
  ;; stanza, but it will read it without passing through leiningen's profile
  ;; merging. So don't put a :figwheel section under the :dev profile, it will
  ;; not be picked up, instead configure figwheel here on the top level.

  :figwheel {;; :http-server-root "public"       ;; serve static assets from resources/public/
             ;; :server-port 3449                ;; default
             ;; :server-ip "127.0.0.1"           ;; default
             :css-dirs ["resources/public/css"]  ;; watch and update CSS

             ;; Instead of booting a separate server on its own port, we embed
             ;; the server ring handler inside figwheel's http-kit server, so
             ;; assets and API endpoints can all be accessed on the same host
             ;; and port.
             ;; This option is disabled because it causes a lot of problems, notably:
             ;; - (reset) does not work properly, the code can't be replaced and the handler
             ;;   keeps using the old database connection (which has been released already)
             ;; - Huge reload time because of cljs recompilation, useless when only working
             ;;   with the backend. When one is working with the frontend (reset) is not necessary,
             ;;   so waiting for the cljs code to compile shouldn't be necessary most of the time
             ;; :ring-handler user/http-handler

             ;; Start an nREPL server into the running figwheel process. We
             ;; don't do this, instead we do the opposite, running figwheel from
             ;; an nREPL process, see
             ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
             ;; :nrepl-port 7888

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             :open-file-command "open-with-subl3"

             :server-logfile "log/figwheel.log"
             :repl false
             }

  :doo {:build "test"}


  :sassc [{:src "src/scss/style.scss"
           :output-to "resources/public/css/style.css"}]

  :auto {"sassc" {:file-pattern  #"\.(scss)$"
                  :paths ["src/scss"]}}

  :profiles { :dev {:dependencies [[figwheel "0.5.8"]
                                   [figwheel-sidecar "0.5.8"]
                                   [com.cemerick/piggieback "0.2.1"]
                                   [org.clojure/tools.nrepl "0.2.12"]
                                   [com.cemerick/pomegranate "0.3.1"]
                                   [org.clojure/test.check "0.9.0"]
                                   [com.gfredericks/test.chuck "0.2.7"]]

                    :plugins [[lein-figwheel "0.5.4-4"]
                              [lein-doo "0.1.6"]]

                    :source-paths ["dev"]}

              :uberjar {:source-paths ^:replace ["src/clj" "src/cljc" "custom-lib"]
                        :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                        :hooks [leiningen.sassc]
                        :omit-source true
                        :aot :all}})
