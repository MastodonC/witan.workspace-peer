(ns onyx.plugin.redis
  (:require [clojure.core.async :as async :refer [timeout]]
            [onyx.peer function
             [pipeline-extensions :as p-ext]]
            [onyx.static
             [default-vals :refer [arg-or-default]]
             [uuid :refer [random-uuid]]]
            [onyx.types :as t]
            [taoensso.carmine :as car :refer [wcar]]
            [onyx.tasks.redis :as redis]))

(defn redis-get
  [conn k]
  (wcar conn (car/get k)))

(defn redis-set!
  [conn k v]
  (wcar conn (car/set k v)))

(defn redis-rpush!
  [conn k v]
  (wcar conn (car/rpush k v)))

(defn redis-lrange!
  [conn k c]
  (try
    (wcar conn (car/lrange k 0 c))
    (catch Throwable e
      ;;need to narrow this exceptions scope
      ;; if thrown, then the key has changed type, probably to done string
      (redis-get conn k))))

(defn redis-del!
  [conn k]
  (wcar conn (car/del k)))

(defn key-exists!
  [conn k]
  (= (wcar conn (car/exists k)) 1))

(defn sentinel?
  [conn k s]
  (let [t (wcar conn (car/type k))]
    (and (= t "string")
         (= s (redis-get conn k)))))

(def cmds
  {:redis/rpush redis-rpush!
   :redis/set redis-set!
   :redis/get redis-get
   :redis/lrange redis-lrange!})

;;;;;;;;;;;;;;;;;;;;
;; Connection lifecycle code

(defn inject-conn-spec [{:keys [onyx.core/params onyx.core/task-map] :as event}
                        {:keys [redis/param? redis/uri redis/read-timeout-ms] :as lifecycle}]
  (when-not (or uri (:redis/uri task-map))
    (throw (ex-info ":redis/uri must be supplied to output task." lifecycle)))
  (let [conn {:spec {:uri (or (:redis/uri task-map) uri)
                     :read-timeout-ms (or read-timeout-ms 4000)}}]
    {:onyx.core/params (if (or (:redis/param? task-map) param?)
                         (conj params conn)
                         params)
     :redis/conn conn}))

(def reader-conn-spec
  {:lifecycle/before-task-start inject-conn-spec})

(defn event->batch
  [event]
  (mapcat :leaves (:tree (:onyx.core/results event))))

(def sentinel "done")

;;;;;;;;;;;;;;;;;;;;;
;; Output plugin code

(defrecord RedisWriter [conn cmd k]
  p-ext/Pipeline
  (read-batch [_ event]
    (onyx.peer.function/read-batch event))

  (write-batch [_ event]
    (when-let [segment (first (event->batch event))]
      (let [package (select-keys segment [:id :message])]
        (cmd conn k package)))
    {})
  (seal-resource [_ _]
    {}))

(defn writer
  [pipeline-data]
  (let [catalog-entry (:onyx.core/task-map pipeline-data)
        uri (:redis/uri catalog-entry)
        k (:redis/key catalog-entry)
        cmd ((:redis/cmd catalog-entry) cmds)
        _ (when-not uri
            (throw (ex-info ":redis/uri must be supplied to output task." catalog-entry)))
        conn {:spec {:uri uri
                     :read-timeout-ms (or (:redis/read-timeout-ms catalog-entry)
                                          4000)}}]
    (->RedisWriter conn cmd k)))

;;;;;;;;;;;;;;;;;;;;;
;; Input plugin code

(defn inject-pending-state [event lifecycle]
  (let [pipeline (:onyx.core/pipeline event)
        task     (:onyx.core/task-map event)]
    (when (> (:onyx/max-peers task) 1)
      (throw (Exception. "Onyx-Redis can not run with :onyx/max-peers greater than 1")))
    {:redis/conn             (:conn pipeline)
     :redis/drained?         (:drained? pipeline)
     :redis/pending-messages (:pending-messages pipeline)}))

(defrecord RedisReader [conn k cmd length unacked-messages drained? current-state]
  p-ext/Pipeline
  p-ext/PipelineInput
  (write-batch [this event]
    (onyx.peer.function/write-batch event))

  (read-batch [_ event]
    (when-not @drained?
      (let [{:keys [message]
             :as raw-state} 
            (if length 
              (cmd conn k length)
              (cmd conn k))
            id (random-uuid)]
        (when (and (not= raw-state @current-state)
                   (if length
                     (or
                      (= "done" raw-state)
                      (= length (count raw-state)))
                     true))
          (reset! current-state raw-state)
          (let [done? (= "done" raw-state)
                ret   {:onyx.core/batch [(t/input id (if done? :done (if length
                                                                       (apply merge (map :message raw-state))
                                                                       message)))]}]
            (reset! drained? done?)
            (if done?
              (redis-del! conn k)
              (swap! unacked-messages assoc id raw-state))
            ret)))))

  (ack-segment
      [_ e message-id]
      (let [m (get @unacked-messages message-id)]
        (when (= (:id m)
                 (:id (if length 
                        (cmd conn k length)
                        (cmd conn k))))
          (redis-set! conn k sentinel)))
      (swap! unacked-messages dissoc message-id))

  (retry-segment
      [_ _ message-id]
      (when-let [m (get @unacked-messages message-id)]
        (swap! unacked-messages dissoc message-id)
        (redis-set! conn k m)
        (reset! current-state nil)
        (reset! drained? false)))

  (pending?
      [_ _ message-id]
      (contains? @unacked-messages message-id))

  (drained?
      [_ event]
      @drained?)

  (seal-resource
      [_ event]
      {}))

(defn reader [pipeline-data]
  (let [catalog-entry (:onyx.core/task-map pipeline-data)
        drained?      (atom false)
        read-timeout  (or (:redis/read-timeout-ms catalog-entry) 4000)
        k (:redis/key catalog-entry)
        uri (:redis/uri catalog-entry)
        unacked-messages (atom {})
        cmd ((:redis/cmd catalog-entry) cmds)
        length (:redis/length catalog-entry)
        _ (when-not uri
            (throw (ex-info ":redis/uri must be supplied to output task." catalog-entry)))
        conn             {:pool nil
                          :spec {:uri uri
                                 :read-timeout-ms read-timeout}}]
    (->RedisReader conn k
                   cmd length
                   unacked-messages
                   drained? (atom nil))))

(def reader-state-calls
  {:lifecycle/before-task-start inject-pending-state})
