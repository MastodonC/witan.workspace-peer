(ns witan.workspace-peer.utils.workflow)

(defn ns-workflowfns
  "Fetches exported workflowfns from a ns"
  [ns-sym]
  (->> ns-sym
       (ns-publics)
       (filter #(-> % second meta :witan/workflowfn :witan/exported?))
       (mapv (juxt (comp str second) (comp :witan/workflowfn meta second)))))

(defn ns-workflowmodels
  "Fetches exported workflowmodels from a ns"
  [ns-sym]
  (->> ns-sym
       (ns-publics)
       (filter #(-> % second meta :witan/workflowmodel))
       (mapv (juxt (comp str second) (comp :witan/workflowmodel meta second)))))

(defn ns-workflowpredicates
  "Fetches exported workflowpreds from a ns"
  [ns-sym]
  (->> ns-sym
       (ns-publics)
       (filter #(-> % second meta :witan/workflowpred :witan/exported?))
       (mapv (juxt (comp str second) (comp :witan/workflowpred meta second)))))
