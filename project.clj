(defproject ventas-core "0.0.14-SNAPSHOT"
  :description "Shared code for ventas components"

  :url "https://github.com/joelsanchez/ventas"

  :scm {:url "git@github.com:joelsanchez/ventas.git"}

  :pedantic? :abort

  :author {:name "Joel Sánchez"
           :email "webmaster@kazer.es"}

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :deploy-repositories {"releases" {:url "https://clojars.org/repo"
                                    :sign-releases false
                                    :username :env
                                    :password :env}
                        "snapshots" {:url "https://clojars.org/repo"
                                     :sign-releases false
                                     :username :env
                                     :password :env}}

  :repositories {"my.datomic.com"
                 ~(merge
                    {:url "https://my.datomic.com/repo"}
                    (let [username (System/getenv "DATOMIC__USERNAME")
                          password (System/getenv "DATOMIC__PASSWORD")]
                      (when (and username password)
                        {:username username
                         :password password})))}

  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/core.async "0.4.490"]

                 [com.google.guava/guava "21.0"]

                 ;; Spec stuff
                 [expound "0.7.2"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [metosin/spec-tools "0.8.3" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [com.gfredericks/test.chuck "0.2.9"]

                 ;; Logging (via logback)
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [ch.qos.logback/logback-core "1.2.3"]
                 [org.clojure/tools.logging "0.4.1"]

                 [jarohen/chord "0.8.1" :exclusions [net.unit8/fressian-cljs org.clojure/tools.reader]]
                 [cheshire "5.8.1"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [org.clojure/data.fressian "0.2.1"]

                 ;; Server-side HTTP requests
                 [clj-http "3.9.1" :exclusions [riddley]]

                 ;; HTTP server, routing
                 [http-kit "2.3.0"]
                 [compojure "1.6.1" :exclusions [instaparse]]

                 ;; Authentication
                 [buddy "2.0.0" :exclusions [instaparse]]

                 ;; Ring
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.2"]
                 [bk/ring-gzip "0.3.0"]
                 [ring/ring-json "0.4.0" :exclusions [cheshire]]

                 [fogus/ring-edn "0.3.0"]
                 [prone "1.6.1"]

                 ;; Configuration
                 [cprop "0.1.13"]

                 ;; i18n
                 [tongue "0.2.6"]

                 ;; HTML templating
                 [selmer "1.12.5" :exclusions [cheshire joda-time]]

                 ;; component alternative
                 [mount "0.1.15"]

                 ;; Filesystem utilities
                 [me.raynes/fs "1.4.6"]

                 ;; "throw+" and "try+"
                 [slingshot "0.12.2"]

                 ;; String manipulation
                 [funcool/cuerdas "2.1.0"]

                 ;; Collection manipulation
                 [com.rpl/specter "1.1.3-SNAPSHOT" :exclusions [riddley]]

                 ;; Database
                 [com.datomic/datomic-free "0.9.5697" :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]
                 [io.rkn/conformity "0.5.1"]

                 ;; Uploads
                 [byte-streams "0.2.4"]

                 ;; Image processing
                 [net.coobird/thumbnailator "0.4.7"]

                 ;; ZIP
                 [net.lingala.zip4j/zip4j "1.3.2"]

                 ;; Elasticsearch
                 [cc.qbits/spandex "0.6.4" :exclusions [ring/ring-codec]]

                 ;; DateTime
                 [clj-time "0.15.1"]

                 ;; Email
                 [com.draines/postal "2.0.3"]

                 ;; Retry
                 [com.grammarly/perseverance "0.1.3"]

                 ;; transitive
                 [org.clojure/tools.reader "1.3.0-alpha3"]

                 ;; CLJS
                 [alandipert/storage-atom "2.0.1"]
                 [bidi "2.1.5"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [joelsanchez/fressian-cljs "0.2.1"]
                 [day8.re-frame/forward-events-fx "0.0.6"]
                 [joelsanchez/ventas-bidi-syntax "0.1.4" :exclusions [org.clojure/core.async]]
                 [re-frame "0.10.6" :exclusions [org.clojure/clojurescript
                                                 org.clojure/tools.logging]]
                 [soda-ash "0.82.2" :exclusions [cljsjs/react-dom cljsjs/react org.clojure/clojurescript]]
                 [venantius/accountant "0.2.4" :exclusions [org.clojure/clojurescript]]
                 [com.cemerick/url "0.1.1"]]

  :plugins [[lein-ancient "0.6.15"]
            [deraen/lein-sass4clj "0.3.1" :exclusions [org.apache.commons/commons-compress]]
            [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character "0.1.0"]
            [com.gfredericks/how-to-ns "0.1.8"]
            [lein-cljfmt "0.5.7" :exclusions [org.clojure/clojure]]
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

  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"
             ;; Disable empty/useless menu item in OSX
             "-Dapple.awt.UIElement=true"]

  :aliases {"fmt" ["with-profile" "dev"
                   "do"
                   ["cljfmt" "fix"]
                   ["all-my-files-should-end-with-exactly-one-newline-character" "so-fix-them"]]}

  :sass {:source-paths ["src/scss"]
         :target-path "resources/public/files/css"
         :source-map true}

  :profiles {:dev {:repl-options {:init-ns repl
                                  :nrepl-middleware [shadow.cljs.devtools.server.nrepl04/cljs-load-file
                                                     shadow.cljs.devtools.server.nrepl04/cljs-eval
                                                     shadow.cljs.devtools.server.nrepl04/cljs-select]
                                  :timeout 120000}
                   :dependencies [[binaryage/devtools "0.9.10"]
                                  [org.clojure/tools.namespace "0.3.0-alpha4"]
                                  [deraen/sass4clj "0.3.1" :exclusions [org.apache.commons/commons-compress]]
                                  [devcards "0.2.4" :exclusions [cljsjs/react cljsjs/react-dom org.clojure/clojurescript]]
                                  [thheller/shadow-cljs "2.8.31" :exclusions [org.clojure/tools.reader
                                                                              org.clojure/clojure
                                                                              com.google.guava/guava
                                                                              org.clojure/tools.cli
                                                                              commons-codec
                                                                              commons-io
                                                                              ring/ring-core]]]
                   :source-paths ["dev/clj" "dev/cljs"]}
             :test {:resource-paths ["test-resources"]}})
