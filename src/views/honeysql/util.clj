(ns views.honeysql.util)

;; The following is used for full refresh views where we can have CTEs and
;; subselects in play.

(defn- first-leaf
  "Retrieves the first leaf in a collection of collections

  (first-leaf :table)                -> :table
  (first-leaf [[:table] [& values]]) -> :table"
  [v]
  (if (coll? v) (recur (first v)) v))

(defn isolate-tables
  "Isolates tables from table definitions in from and join clauses."
  [c]
  (if (keyword? c) c (let [v (first c)] (when (keyword? v) v))))

(defn from-tables
  [from tables]
  (->> from
       (keep isolate-tables)
       (swap! tables #(into %1 %2))))

(defn every-second
  [coll]
  (map first (partition 2 coll)))

(defn join-tables
  [join tables]
  (->> (every-second join)
       (keep isolate-tables)
       (swap! tables #(into %1 %2))))

(defn insert-tables
  [query]
  (some->> query :insert-into first-leaf vector))

(defn update-tables
  [query]
  (if-let [v (:update query)] [v] []))

(defn delete-tables
  [query]
  (if-let [v (:delete-from query)] [v] []))

(defn query-tables
  "Return all the tables in an sql statement."
  [query]
  (let [tables (atom #{})
        get-tables (fn [node]
                     (when (map? node)
                       (when-let [from (:from node)]
                         (from-tables from tables)
                         (doseq [join-key [:join :left-join :right-join :full-join]]
                           (when-let [join (get node join-key)] (join-tables join tables)))))
                     node)]
    (clojure.walk/postwalk get-tables query)
    (-> @tables
        (into (insert-tables query))
        (into (update-tables query))
        (into (delete-tables query)))))
