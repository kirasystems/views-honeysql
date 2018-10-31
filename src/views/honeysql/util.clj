(ns views.honeysql.util)

;; The following is used for full refresh views where we can have CTEs and
;; subselects in play.

(declare query-tables)

(defn- first-leaf
  "Retrieves the first leaf in a collection of collections

  (first-leaf :table)                -> :table
  (first-leaf [[:table] [& values]]) -> :table"
  [v]
  (if (coll? v) (recur (first v)) v))

(defn second-level-tables
  "For HoneySQL constructs where there is a subselect embedded
   in the second value of a vector--CTEs, lateral joins."
  [query clause-key]
  (mapcat #(query-tables (second %)) (get query clause-key)))

(defn isolate-tables
  "Isolates tables from table definitions in from and join clauses."
  [c]
  (if (keyword? c) [c] (let [v (first c)] (if (map? v) (query-tables v) [v]))))

(defn from-tables
  [query]
  (mapcat isolate-tables (:from query)))

(defn every-second
  [coll]
  (map first (partition 2 coll)))

(defn join-tables
  [query k]
  (mapcat isolate-tables (every-second (k query))))

(defn collect-maps
  [wc]
  (cond
    (coll? wc) (let [maps  (filterv map? wc)
                     colls (filter #(and (coll? %) (not (map? %))) wc)]
                 (into maps (mapcat collect-maps colls)))
    (map? wc)  [wc]
    :else      []))

(defn select-tables
  "Search for subqueries in the select clause."
  [query]
  (mapcat query-tables (collect-maps (:select query))))

(defn where-tables
  "This search for subqueries in the where clause."
  [query]
  (mapcat query-tables (collect-maps (:where query))))

(defn insert-tables
  [query]
  (some->> query :insert-into first-leaf vector))

(defn update-tables
  [query]
  (if-let [v (:update query)] [v] []))

(defn delete-tables
  [query]
  (if-let [v (:delete-from query)] [v] []))

(defn set-operations-tables
  "return tables used in operands for the set operation op

  note: HoneySQL currently support INTERSECT, UNION, UNION ALL as set
  operations."
  [query op]
  (mapcat query-tables (op query)))

(defn exists-tables
  [query]
  (when (:exists query)
    (query-tables (:exists query))))

(defn query-tables
  "Return all the tables in an sql statement."
  [query]
  (set (concat
        (second-level-tables query :with)
        (second-level-tables query :with-recursive)
        (second-level-tables query :join-lateral)
        (second-level-tables query :left-join-lateral)
        (from-tables query)
        (join-tables query :join)
        (join-tables query :left-join)
        (join-tables query :right-join)
        (select-tables query)
        (where-tables query)
        (insert-tables query)
        (update-tables query)
        (delete-tables query)
        (set-operations-tables query :intersect)
        (set-operations-tables query :union-all)
        (set-operations-tables query :union)
        (exists-tables query))))
