(ns witan.workspace-peer.launcher.web-server
  (:require [org.httpkit.server :refer [run-server]]
            [cheshire.core :refer :all]
            [witan.workspace-peer.config :as c]
            [cognitect.transit :as t]
            [outpace.schema-transit :as st])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def server (atom nil))
(defn json-response
  [x]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (generate-string x)})


(defn transitize
  [x]
  (let [out (ByteArrayOutputStream. 4096)]
    (t/write
     (t/writer out
               :json-verbose
               {:handlers st/write-handlers})
     x)
    (ByteArrayInputStream. (.toByteArray out))))

(defn respond
  [r]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (transitize r)})

(defmulti route-me
  (fn [{:keys [request-method uri]}] [request-method uri]))

(defmethod route-me
  [:get "/functions"]
  [req]
  (respond
   {:functions (c/workflow-fns)}))

(defmethod route-me
  [:get "/models"]
  [req]
  (respond
   {:models (c/workflow-models)}))

(defmethod route-me
  [:get "/predicates"]
  [req]
  (respond
   {:predicates (c/workflow-predicates)}))

(defmethod route-me
  :default
  [req]
  {:status 404})

(defn run
  [port]
  (reset! server (run-server route-me {:port port})))

(defn stop
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))
