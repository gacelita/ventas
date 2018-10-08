(def aot-namespaces
  ['clojure.tools.logging.impl
   'ventas.core])

(def prepare-uberjar
  ["run" "-m" "ventas-devtools.uberjar/prepare" ":main" :project/main])

(defproject ventas "0.0.11-SNAPSHOT"
  :description "The Ventas eCommerce platform"

  :url "https://github.com/JoelSanchez/ventas"

  :scm {:url "git@github.com:joelsanchez/ventas.git"}

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
                  :creds :gpg}
                 "snapshots"
                 {:url "https://repo.clojars.org"
                  :creds :gpg}}

  :dependencies [
                 ;; Clojure
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339" :scope "provided"]
                 [org.clojure/core.async "0.4.474" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/tools.nrepl "0.2.13"]

                 ;; Spec stuff
                 [expound "0.7.1"]
                 [org.clojure/spec.alpha "0.1.143" :scope "provided"]
                 [metosin/spec-tools "0.7.2"]
                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.9"]

                 [com.google.guava/guava "21.0"]

                 ;; Namespace tools
                 [org.clojure/tools.namespace "0.3.0-alpha4"]

                 ;; Logging
                 ;; We use timbre for logging, so we redirect everything to slf4j
                 ;; and then we redirect slf4j to timbre
                 [com.fzakaria/slf4j-timbre "0.3.12"]
                 [com.taoensso/timbre       "4.10.0"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]

                 ;; JSON, Transit and Fressian
                 [jarohen/chord "0.8.1" :exclusions [net.unit8/fressian-cljs]]
                 [org.clojure/data.json "0.2.6"]
                 [cheshire "5.8.1"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [org.clojure/data.fressian "0.2.1"]
                 [joelsanchez/fressian-cljs "0.2.1"]

                 ;; Server-side HTTP requests
                 [clj-http "3.9.1" :exclusions [riddley]]

                 ;; HTTP server, routing
                 [http-kit "2.3.0"]
                 [compojure "1.6.1"]

                 ;; Authentication
                 [buddy "2.0.0"]

                 ;; Ring
                 [ring "1.7.0"]
                 [ring/ring-defaults "0.3.2"]
                 [bk/ring-gzip "0.3.0"]
                 [ring/ring-json "0.4.0" :exclusions [cheshire]]
                 [fogus/ring-edn "0.3.0"]
                 [prone "1.6.0"]

                 ;; Configuration
                 [cprop "0.1.13"]

                 ;; i18n
                 [tongue "0.2.4"]

                 ;; kafka
                 [spootnik/kinsky "0.1.22"]

                 ;; HTML templating
                 [selmer "1.12.1" :exclusions [cheshire joda-time]]

                 ;; component alternative
                 [mount "0.1.13"]

                 ;; Filesystem utilities
                 [me.raynes/fs "1.4.6"]

                 ;; "throw+" and "try+"
                 [slingshot "0.12.2"]

                 ;; String manipulation
                 [funcool/cuerdas "2.0.6"]

                 ;; Collection manipulation
                 [com.rpl/specter "1.1.1" :exclusions [riddley]]

                 ;; Database
                 [io.rkn/conformity "0.5.1"]

                 ;; Text colors in the console
                 [io.aviso/pretty "0.1.35"]

                 ;; UUIDs
                 [danlentz/clj-uuid "0.1.7" :exclusions [primitive-math]]

                 ;; Uploads
                 [byte-streams "0.2.4"]

                 ;; Image processing
                 [fivetonine/collage "0.2.1"]

                 ;; Elasticsearch
                 [cc.qbits/spandex "0.6.4"]

                 ;; Server-side prerendering
                 [etaoin "0.2.9"]

                 ;; DateTime
                 [clj-time "0.14.4"]

                 ;; Email
                 [com.draines/postal "2.0.2"]

                 ;; Stripe
                 [abengoa/clj-stripe "1.0.4"]

                 ;; Retry
                 [com.grammarly/perseverance "0.1.3"]]

  :plugins [[lein-ancient "0.6.15"]
            [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.0"]
            [com.gfredericks/how-to-ns "0.1.8"]
            [lein-cljfmt "0.5.7"]
            [lein-cloverage "1.0.10"]]

  :how-to-ns {:require-docstring?      false
              :sort-clauses?           true
              :allow-refer-all?        false
              :allow-extra-clauses?    false
              :align-clauses?          false
              :import-square-brackets? true}

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

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

  :repl-options {:init-ns repl
                 :port 4001
                 :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                 :timeout 120000}

  :aliases {"nrepl" ["repl" ":connect" "localhost:4001"]
            "install" ["do" ["clean"] ["with-profile" "datomic-pro,build-client" "install"]]
            "release" ["do" ["clean"] ["with-profile" "datomic-free,build-client" "release"]]
            "deploy" ["do" ["clean"] ["with-profile" "datomic-free,build-client" "deploy"]]
            "prepare" ["do" ["clean"] ["with-profile" "ventas-devtools,cljs-deps,datomic-pro" ~prepare-uberjar]]
            "compile-cljs-tests" ["do" ["clean"] ["with-profile" "ventas-devtools,cljs-deps,datomic-free" "run" "-m" "ventas-devtools.karma/compile"]]
            "test" ["with-profile" "datomic-free" "test"]
            "fmt" ["with-profile" "fmt" "do" ["cljfmt" "fix"] ["all-my-files-should-end-with-exactly-one-newline-character" "so-fix-them"]]}

  :main ventas.core

  :ultra {:repl false}

  :profiles {:datomic-pro ^:leaky {:dependencies [[com.datomic/datomic-pro "0.9.5697" :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]]}
             :datomic-free ^:leaky {:dependencies [[com.datomic/datomic-free "0.9.5697" :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]]}
             :ventas-devtools {:dependencies [[ventas/devtools "0.0.11-SNAPSHOT"]]}
             :cljs-deps {:dependencies [[alandipert/storage-atom "2.0.1"]
                                        [bidi "2.1.4"]
                                        [day8.re-frame/forward-events-fx "0.0.6"]
                                        [devcards "0.2.4" :exclusions [cljsjs/react]]
                                        [joelsanchez/ventas-bidi-syntax "0.1.2"]
                                        [reagent "0.8.1"]
                                        [re-frame "0.10.6"]
                                        [soda-ash "0.82.2" :exclusions [cljsjs/react-dom cljsjs/react]]
                                        [thheller/shadow-cljs "2.6.10"]
                                        [venantius/accountant "0.2.4"]
                                        [com.cemerick/url "0.1.1"]]}
             :development {:dependencies [[org.codehaus.plexus/plexus-utils "3.0.15"]
                                          [cider/piggieback "0.3.9"]
                                          [binaryage/devtools "0.9.10"]
                                          [com.cemerick/pomegranate "1.0.0"]]
                           :plugins [[cider/cider-nrepl "0.19.0-SNAPSHOT" :exclusions [org.clojure/tools.nrepl]]
                                     [refactor-nrepl "2.4.0-SNAPSHOT" :exclusions [org.clojure/tools.nrepl]]
                                     [venantius/ultra "0.5.2"]]
                           :source-paths ["dev/clj" "dev/cljs"]}

             :fmt {:source-paths ^:replace ["dev/clj" "dev/cljs" "src/clj" "src/cljc" "src/cljs"]}
             :build-client ^:leaky [:ventas-devtools :cljs-deps {:prep-tasks ["javac" "compile" ~prepare-uberjar]}]
             :repl ^:repl [:datomic-pro :development :ventas-devtools :cljs-deps]
             :uberjar [:datomic-pro :build-client {:source-paths ^:replace ["src/clj" "src/cljc"]
                                                   :omit-source true
                                                   :aot ~aot-namespaces}]})
