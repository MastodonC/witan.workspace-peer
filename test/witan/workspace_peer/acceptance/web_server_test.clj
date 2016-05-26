(ns witan.workspace-peer.acceptance.web-server-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [schema.core :as s]
            [org.httpkit.client :as client]
            [witan.workspace-peer.config :as c]
            [witan.workspace-peer.launcher.web-server :as server]
            [clojure.data.json :as json]
            [cognitect.transit :as t]
            [outpace.schema-transit :as st]))

(def port 8291)

(defn start-web
  [f]
  (server/run port)
  (f)
  (server/stop))

(use-fixtures :once start-web)

(defn transit-read [in]
  (t/read (t/reader in
                    :json-verbose
                    {:handlers st/read-handlers})))

(defn get-resource
  []
  (-> (client/get (str "http://localhost:" port "/functions") {:as :byte-array})
      deref
      :body
      io/input-stream
      transit-read))



(deftest schemas-returned-usable
  (is
   (= (c/workflow-fns)
      (:functions (get-resource)))))
