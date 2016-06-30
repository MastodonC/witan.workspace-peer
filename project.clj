(defproject witan.workspace-peer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[aero "0.1.5" :exclusions [prismatic/schema]]
                 [amazonica "0.3.61"]
                 [cheshire "5.5.0"]
                 [mysql/mysql-connector-java "5.1.18"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.onyxplatform/onyx "0.9.4"]
                 [org.onyxplatform/onyx-kafka "0.9.4.0"]
                 [org.onyxplatform/onyx-seq "0.9.4.0"]
                 [org.onyxplatform/onyx-sql "0.9.4.0"]
                 [org.onyxplatform/onyx-metrics "0.9.4.0"]
                 [com.outpace/schema-transit "0.2.3"]
                 [com.rpl/specter "0.9.3"]
                 [com.taoensso/timbre "4.3.1"]
                 [com.taoensso/carmine "2.12.2" :exclusions [com.taoensso/timbre com.taoensso/encore]]
                 [http-kit "2.1.18"]
                 [cheshire "5.6.1"]
                 ;;
                 [witan.workspace-api "0.1.4"]
                 [witan.workspace-onyx-plugins "0.1.0"]
                 ;; Add function and model libraries here
                 [witan.models.demography "0.1.0-SNAPSHOT" :exclusions [witan.workspace-api]]]

  :plugins [[lein-project-version "0.1.0"]]

  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)}

  :profiles {:uberjar {:aot [witan.workspace-peer.launcher.aeron-media-driver
                             witan.workspace-peer.launcher.launch-prod-peers]
                       :uberjar-name "witan.workspace-peer-standalone.jar"}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [lein-project-version "0.1.0"]]
                   :source-paths ["src"]}})
