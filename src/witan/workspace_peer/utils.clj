(ns witan.workspace-peer.utils)

(defmacro condas->
  "A mixture of cond-> and as-> allowing more flexibility in the test and step forms"
  [expr name & clauses]
  (assert (even? (count clauses)))
  (let [pstep (fn [[test step]] `(if ~test ~step ~name))]
    `(let [~name ~expr
           ~@(interleave (repeat name) (map pstep (partition 2 clauses)))]
       ~name)))

(defn find-dupes
  [coll]
  (->> coll
       (reduce #(assoc %1 %2 (inc (%1 %2 0))) {})
       (keep (fn [[i c]] (when (> c 1) i)))
       (not-empty)))
