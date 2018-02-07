
(defn minified-build [theme]
  (let [name (name theme)]
    {:id (str "min-" name)
     :source-paths ["src/cljs" "src/cljc" "custom-lib"]
     :compiler {:main (symbol (str "ventas.themes." name ".core"))
                :output-to (str "resources/public/files/js/compiled/" name ".js")
                :output-dir (str "resources/public/files/js/compiled/" name)
                :source-map-timestamp true
                :optimizations :advanced
                :pretty-print false
                :externs ["externs.js"]
                :parallel-build true}}))

(def aot-namespaces
  ['clojure.tools.logging.impl
   'ventas.core])

(defproject ventas "0.0.2-SNAPSHOT"
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
                         :password password})))}

  :dependencies [
                 ;; Clojure
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [org.clojure/core.async "0.4.474" :exclusions [org.clojure/tools.reader]]
                 [expound "0.4.0"]
                 [org.clojure/spec.alpha "0.1.143" :scope "provided"]
                 [metosin/spec-tools "0.6.0-SNAPSHOT"]
                 [com.google.guava/guava "23.0"]
                 [org.clojure/tools.nrepl "0.2.13"]

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
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [org.clojure/data.fressian "0.2.1"]

                 ;; Server-side HTTP requests
                 [clj-http "3.7.0" :exclusions [riddley]]

                 ; HTTP server
                 [http-kit "2.2.0"]

                 ; Authentication
                 [buddy "2.0.0" :exclusions [instaparse]]

                 ;; Ring
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [bk/ring-gzip "0.2.1"]
                 [ring/ring-json "0.4.0" :exclusions [cheshire]]

                 ;; Routing
                 [compojure "1.6.0" :exclusions [instaparse]]

                 ;; Configuration
                 [cprop "0.1.11"]

                 ;; i18n
                 [tongue "0.2.3"]

                 ;; re-frame
                 [reagent "0.7.0"]
                 [re-frame "0.10.2"]
                 [day8.re-frame/forward-events-fx "0.0.5"]

                 ;; Semantic UI
                 [soda-ash "0.76.0"]

                 ; Routing
                 [bidi "2.1.3"]
                 [venantius/accountant "0.2.3"]

                 ; HTML templating
                 [selmer "1.11.5" :exclusions [cheshire joda-time]]

                 ;; component alternative
                 [mount "0.1.11"]

                 ;; Process starting and stopping
                 [me.raynes/conch "0.8.0"]

                 ;; Database
                 [com.datomic/datomic-pro "0.9.5561.56" :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]
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
                 [clj-time "0.14.2"]

                 ;; localStorage
                 [alandipert/storage-atom "2.0.1"]

                 ;; Generators
                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.8" :exclusions [instaparse]]

                 ;; Image processing
                 [fivetonine/collage "0.2.1"]

                 [binaryage/devtools "0.9.9"]

                 ;; String manipulation
                 [funcool/cuerdas "2.0.5"]

                 ;; Elasticsearch
                 [cc.qbits/spandex "0.5.5"]

                 ;; Server-side prerendering
                 [etaoin "0.2.5"]

                 ; Error reporting for Ring
                 [prone "1.2.0"]

                 ;; Devcards itself
                 [devcards "0.2.4" :exclusions [cljsjs/react]]]

  :plugins [[lein-ancient "0.6.14"]
            [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.0"]
            [com.gfredericks/how-to-ns "0.1.8"]
            [lein-auto "0.1.3"]
            [lein-cljfmt "0.5.6"]
            [lein-cljsbuild "1.1.7"]
            [lein-cloverage "1.0.7-SNAPSHOT"]
            [lein-sassc "0.10.4" :exclusions [org.apache.commons/commons-compress org.clojure/clojure]]
            [venantius/ultra "0.5.2" :exclusions [org.clojure/clojure]]]

  :cljfmt {:file-pattern #"(src|test)\/.*?\.clj[sx]?$"}

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
            "release-deploy" ["with-profile" "release" "deploy clojars"]
            "release-install" ["with-profile" "release" "install"]
            "compile-min" ["do" ["clean"] ["cljsbuild" "once" "min"]]}

  :cljsbuild {:builds
              [
               ;; This build will be altered by client/dev-build, to do theme-dependent
               ;; builds.
               {:id "app"
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

  :profiles {:dev {:dependencies [[figwheel "0.5.14"]
                                  [figwheel-sidecar "0.5.14"]
                                  [com.cemerick/piggieback "0.2.2"]

                                  ; Runtime dependency resolution
                                  [com.cemerick/pomegranate "1.0.0"]
                                  [org.codehaus.plexus/plexus-utils "3.0.15"]

                                  [org.clojure/test.check "0.9.0"]
                                  [com.gfredericks/test.chuck "0.2.8"]]
                   :plugins [[lein-figwheel "0.5.14" :exclusions [org.clojure/clojure]]
                             [lein-doo "0.1.8" :exclusions [org.clojure/clojure]]
                             [cider/cider-nrepl "0.17.0-SNAPSHOT" :exclusions [org.clojure/tools.nrepl]]
                             [refactor-nrepl "2.4.0-SNAPSHOT"]]
                   :source-paths ["dev"]}

             :release {:aot ~aot-namespaces
                       :hooks [leiningen.sassc]}

             :uberjar {:source-paths ^:replace ["src/clj" "src/cljc" "custom-lib"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min-clothing" "min-blank"]]
                       :hooks [leiningen.sassc]
                       :omit-source true
                       :aot ~aot-namespaces}})
