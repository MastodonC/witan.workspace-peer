(ns witan.workspace-peer.utils.workflow)

(defn ns-workflowfns
  "Fetches exported workflowfns from a ns"
  [ns-sym]
  (->> ns-sym
       (ns-publics)
       (filter #(-> % second meta :witan/workflowfn :witan/exported?))
       (mapv (juxt (comp str second) (comp :witan/workflowfn meta second)))))
