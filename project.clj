
(defn minified-build [theme]
  (let [name (name theme)]
    {:id (str "min-" name)
     :source-paths ["src/cljs" "src/cljc" "custom-lib"]
     :compiler {:main (symbol (str "ventas.themes." name ".core"))
                :output-to (str "resources/public/files/js/compiled/" name ".js")
                :output-dir (str "resources/public/files/js/compiled/" name)
                :npm-deps {:js-image-zoom "0.5.0"}
                :install-deps true
                :source-map-timestamp true
                :optimizations :advanced
                :pretty-print false
                :externs ["externs.js"]
                :parallel-build true}}))

(def aot-namespaces
  ['clojure.tools.logging.impl
   'ventas.core])

(defproject ventas "0.0.6"
  :description "The Ventas eCommerce platform"

  :url "https://github.com/JoelSanchez/ventas"

  :author {:name "Joel Sánchez"
           :email "webmaster@kazer.es"}

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repositories {"my.datomic.com"
                 ~(merge
                    {:url "https://my.datomic.com/repo"}
                    (let [username (System/getenv "DATOMIC__USERNAME")
                          password (System/getenv "DATOMIC__PASSWORD")]
                      (when (and username password)
                        {:username username
                         :password password})))
                 "releases"
                 {:url "https://repo.clojars.org"
                  :creds :gpg}}

  :dependencies [
                 ;; Clojure
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238" :scope "provided"]
                 [org.clojure/core.async "0.4.474" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/tools.nrepl "0.2.13"]

                 ;; Spec stuff
                 [expound "0.5.0"]
                 [org.clojure/spec.alpha "0.1.143" :scope "provided"]
                 [metosin/spec-tools "0.6.1"]
                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.9"]

                 ;; Explicit transitive dependencies
                 [com.google.code.findbugs/jsr305 "3.0.1"]
                 [com.google.guava/guava "23.0"]
                 [instaparse "1.4.8"]

                 ;; ZeroMQ
                 [org.zeromq/jeromq "0.3.3"]
                 [org.zeromq/cljzmq "0.1.4" :exclusions [org.zeromq/jzmq]]

                 ;; Namespace tools
                 [org.clojure/tools.namespace "0.3.0-alpha4"]

                 ;; Logging
                 ;; We use timbre for logging, so we redirect everything to slf4j
                 ;; and then we redirect slf4j to timbre
                 [com.fzakaria/slf4j-timbre "0.3.8"]
                 [com.taoensso/timbre       "4.10.0"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]

                 ;; JSON, Transit and Fressian
                 [jarohen/chord "0.8.1"]
                 [org.clojure/data.json "0.2.6"]
                 [cheshire "5.8.0"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [org.clojure/data.fressian "0.2.1"]

                 ;; Server-side HTTP requests
                 [clj-http "3.8.0" :exclusions [riddley]]

                 ;; HTTP server, routing
                 [http-kit "2.3.0"]
                 [compojure "1.6.1"]

                 ;; Authentication
                 [buddy "2.0.0"]

                 ;; Ring
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [bk/ring-gzip "0.3.0"]
                 [ring/ring-json "0.4.0" :exclusions [cheshire]]
                 [prone "1.5.2"]

                 ;; Configuration
                 [cprop "0.1.11"]

                 ;; i18n
                 [tongue "0.2.4"]

                 ;; kafka
                 [spootnik/kinsky "0.1.22"]

                 ;; HTML templating
                 [selmer "1.11.7" :exclusions [cheshire joda-time]]

                 ;; component alternative
                 [mount "0.1.12"]

                 ;; Filesystem utilities
                 [me.raynes/fs "1.4.6"]

                 ;; "throw+" and "try+"
                 [slingshot "0.12.2"]

                 ;; String manipulation
                 [funcool/cuerdas "2.0.5"]

                 ;; Collection manipulation
                 [com.rpl/specter "1.1.0" :exclusions [riddley]]

                 ;; Database
                 [io.rkn/conformity "0.5.1"]

                 ;; Text colors in the console
                 [io.aviso/pretty "0.1.34"]

                 ;; UUIDs
                 [danlentz/clj-uuid "0.1.7" :exclusions [primitive-math]]

                 ;; Uploads
                 [byte-streams "0.2.3"]

                 ;; Image processing
                 [fivetonine/collage "0.2.1"]

                 ;; Elasticsearch
                 [cc.qbits/spandex "0.6.2"]

                 ;; Server-side prerendering
                 [etaoin "0.2.8"]

                 ;; DateTime
                 [clj-time "0.14.3"]

                 ;; Email
                 [com.draines/postal "2.0.2"]

                 ;; Stripe
                 [abengoa/clj-stripe "1.0.4"]

                 ;;
                 ;; CLJS dependencies
                 ;;

                 ;; URL parsing
                 [com.cemerick/url "0.1.1"]

                 ;; re-frame
                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]
                 [day8.re-frame/forward-events-fx "0.0.5"]

                 ;; Stripe
                 [cljsjs/react-stripe-elements "1.4.1-1"]

                 ;; Semantic UI
                 [soda-ash "0.79.1" :exclusions [cljsjs/react-dom cljsjs/react]]

                 ;; Routing
                 [bidi "2.1.3"]
                 [joelsanchez/ventas-bidi-syntax "0.1.2"]
                 [venantius/accountant "0.2.4"]

                 ;; Charts
                 [cljsjs/chartjs "2.7.0-0"]
                 [cljsjs/moment "2.22.0-0"]

                 ;; localStorage
                 [alandipert/storage-atom "2.0.1"]

                 ;; Nice CLJS development tools
                 [binaryage/devtools "0.9.10"]

                 ;; Devcards itself
                 [devcards "0.2.4" :exclusions [cljsjs/react]]

                 ;; Datepicker
                 [cljsjs/react-date-range "0.2.4-0" :exclusions [cljsjs/react]]]

  :plugins [[lein-ancient "0.6.15"]
            [lein-doo "0.1.10"]
            [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.0"]
            [com.gfredericks/how-to-ns "0.1.8"]
            [lein-auto "0.1.3"]
            [lein-cljfmt "0.5.7"]
            [lein-cljsbuild "1.1.7"]
            [lein-cloverage "1.0.10"]
            [lein-sassc "0.10.4" :exclusions [org.apache.commons/commons-compress]]]

  :how-to-ns {:require-docstring?      false
              :sort-clauses?           true
              :allow-refer-all?        false
              :allow-extra-clauses?    false
              :align-clauses?          false
              :import-square-brackets? true}

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs" "src/cljc" "custom-lib"]

  :test-paths ["test/clj" "test/cljc"]

  :jvm-opts ["-Xverify:none"
             "-XX:-OmitStackTraceInFastThrow"
             ;; Disable empty/useless menu item in OSX
             "-Dapple.awt.UIElement=true"]

  :clean-targets ^{:protect false} [:target-path
                                    :compile-path
                                    "resources/public/files/js"
                                    "storage/rendered"]

  :uberjar-name "ventas.jar"

  :main ventas.core

  :repl-options {:init-ns user
                 :port 4001
                 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :timeout 120000}

  :aliases {"nrepl" ["repl" ":connect" "localhost:4001"]
            ;;  "release" ["with-profile" "package,datomic-free" "deploy" "clojars"]
            "local-install" ["with-profile" "package,datomic-pro" "install"]
            "compile-min" ["do" ["clean"] ["cljsbuild" "once" "min"]]
            "do-release" ["with-profile" "package,datomic-free" "release"]
            "fmt" ["with-profile" "fmt" "do" ["cljfmt" "fix"] ["all-my-files-should-end-with-exactly-one-newline-character" "so-fix-them"]]}

  :cljsbuild {:builds
              [
               ;; This build will be altered by client/dev-build, to do theme-dependent
               ;; builds.
               ;; The default `:main` in here is ventas.themes.clothing.core for compatibility
               ;; with the :embed-figwheel? option, but bear in mind that repl/set-theme!
               ;; won't work if you take that path.
               {:id "app"
                :source-paths ["src/cljs" "src/cljc" "test/cljs" "test/cljc" "custom-lib"]
                :figwheel {:on-jsload "ventas.core/on-figwheel-reload"}
                :compiler {:main ventas.core
                           :npm-deps {:js-image-zoom "0.5.0"}
                           :install-deps true
                           :asset-path "files/js/compiled/out"
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
                           :npm-deps {:js-image-zoom "0.5.0"}
                           :install-deps true
                           :optimizations :none
                           :parallel-build true}}

               ;; In development, the :main and :output-to options are changed dynamically
               ;; to change between themes.
               ;; In production this is not an option, that's why every build needs to be
               ;; explicitly specified.
               ~(minified-build :clothing)
               ~(minified-build :blank)]}

  :figwheel {:css-dirs ["resources/public/files/css"]
             :server-logfile "log/figwheel.log"
             :repl false}

  :doo {:build "test"}

  :sassc [{:src "src/scss/main.scss"
           :output-to "resources/public/files/css/style.css"
           :style "nested"
           :import-path "src/scss"}

          {:src "src/scss/email.scss"
           :output-to "resources/public/files/css/email.css"
           :style "nested"
           :import-path "src/scss"}

          ;; Included themes need their own separate build
          {:src "src/scss/themes/blank/core.scss"
           :output-to "resources/public/files/css/themes/blank.css"
           :style "nested"
           :import-path "src/scss"}
          {:src "src/scss/themes/clothing/core.scss"
           :output-to "resources/public/files/css/themes/clothing.css"
           :style "nested"
           :import-path "src/scss"}]

  :auto {"sassc" {:file-pattern  #"\.(scss)$"
                  :paths ["src/scss"]}}

  :profiles {:datomic-pro {:dependencies [[com.datomic/datomic-pro "0.9.5561.56" :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]]}
             :datomic-free {:dependencies [[com.datomic/datomic-free "0.9.5561.56" :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]]}
             :dev [:datomic-pro {:dependencies [[figwheel "0.5.15"]
                                                [figwheel-sidecar "0.5.15"]
                                                [com.cemerick/piggieback "0.2.2"]

                                                ;; Runtime dependency resolution
                                                [com.cemerick/pomegranate "1.0.0"]
                                                [org.codehaus.plexus/plexus-utils "3.0.15"]]
                                 :plugins [[lein-figwheel "0.5.15"]
                                           [cider/cider-nrepl "0.17.0-SNAPSHOT" :exclusions [org.clojure/tools.nrepl]]
                                           [refactor-nrepl "2.4.0-SNAPSHOT"]]
                                 :source-paths ["dev"]}]

             :repl [:datomic-pro {:plugins [[venantius/ultra "0.5.2"]]}]

             :fmt {:source-paths ^:replace ["dev" "src/clj" "src/cljc" "src/cljs"]}

             :package {:source-paths ^:replace ["src/clj" "src/cljc" "custom-lib"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min-clothing" "min-blank"]]
                       :hooks [leiningen.sassc]
                       :omit-source true
                       :aot ~aot-namespaces}
             
             :uberjar [:datomic-pro :package]})
