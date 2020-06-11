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

(defn from-join-tables
  [node from tables]
  (from-tables from tables)
  (doseq [join-key [:join :left-join :right-join :full-join]]
    (when-let [join (get node join-key)] (join-tables join tables))))

(defn insert-tables
  [insert tables]
  (when-let [v (first-leaf insert)]
    (swap! tables conj v)))

(defn delete-tables
  [node delete-from tables]
  (from-tables [delete-from] tables)
  (when-let [v (:using node)]
    (from-tables v tables)))

(defn query-tables
  "Return all the tables in an sql statement."
  [query]
  (let [tables (atom #{})
        get-tables (fn [node]
                     (when (map? node)
                       (let [{:keys [from insert-into update delete-from]} node]
                         (when update      (from-join-tables node [update] tables))
                         (when from        (from-join-tables node from tables))
                         (when insert-into (insert-tables insert-into tables))
                         (when delete-from (delete-tables node delete-from tables))))
                     node)]
    (clojure.walk/postwalk get-tables query)
    @tables))

