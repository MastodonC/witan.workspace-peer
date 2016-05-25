(ns witan.workspace-peer.launcher.web-server
  (:require [org.httpkit.server :refer [run-server]]
            [cheshire.core :refer :all]
            [witan.workspace-peer.config :as c]))

(def server (atom nil))
(defn json-response
  [x]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (generate-string x)})

(defmulti route-me
  (fn [{:keys [request-method uri]}] [request-method uri]))

(defmethod route-me
  [:get "/functions"]
  [req]
  (json-response {:functions (c/get-functions)}))

(defmethod route-me
  [:get "/models"]
  [req]
  (json-response {:models [4 5 6]}))

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
