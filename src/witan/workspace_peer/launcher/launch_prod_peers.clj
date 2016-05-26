(ns witan.workspace-peer.launcher.launch-prod-peers
  (:gen-class)
  (:require [aero.core :refer [read-config]]
            [clojure.core.async :refer [<!! chan]]
            [witan.workspace-peer.jobs.meetup-job] ;; TODO remove me but include any models and tasks.
            [onyx.plugin.kafka]
            [onyx.plugin.sql]
            [taoensso.timbre :as t]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [onyx.lifecycle.metrics.metrics]
            [onyx.lifecycle.metrics.timbre]
            [witan.workspace-peer.launcher.web-server :as web]))

(defn standard-out-logger
  "Logger to output on std-out, for use with docker-compose"
  [data]
  (let [{:keys [output-fn]} data]
    (println (output-fn data))))

(defn -main [n & args]
  (let [n-peers (Integer/parseInt n)
        config (read-config (clojure.java.io/resource "config.edn") {:profile :default})
        peer-config (-> (:peer-config config)
                        (assoc :onyx.log/config {:appenders
                                                 {:rotor (-> (rotor/rotor-appender
                                                              {:path "onyx.log"
                                                               :max-size (* 512 102400)
                                                               :backlog 5})
                                                             (assoc :min-level :info))
                                                  :standard-out
                                                  {:enabled? true
                                                   :async? false
                                                   :output-fn t/default-output-fn
                                                   :fn standard-out-logger}}}))
        peer-group (onyx.api/start-peer-group peer-config)
        env (onyx.api/start-env (:env-config config))
        peers (onyx.api/start-peers n-peers peer-group)]
    (t/info "Attempting to connect to Zookeeper @" (:zookeeper/address peer-config))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread.
                       (fn []
                         (doseq [v-peer peers]
                           (onyx.api/shutdown-peer v-peer))
                         (onyx.api/shutdown-peer-group peer-group)
                         (shutdown-agents)
                         (web/stop))))
    (t/info "Started peers. Starting web server...")
    ;; Start web server.
    (web/run 8080)
    (t/info "Started web sever. Blocking forever.")
    ;; Block forever.
    (<!! (chan))))
