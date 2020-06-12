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
       (into tables)))

(defn every-second
  [coll]
  (map first (partition 2 coll)))

(defn join-tables
  [join tables]
  (->> (every-second join)
       (keep isolate-tables)
       (into tables)))

(defn from-join-tables
  [node from tables]
  (let [{:keys [join left-join right-join full-join]} node]
    (cond->> (from-tables from tables)
      join       (join-tables join      )
      left-join  (join-tables left-join )
      right-join (join-tables right-join)
      full-join  (join-tables full-join ))))

(defn insert-tables
  [insert tables]
  (if-let [v (first-leaf insert)]
    (conj tables v)
    tables))

(defn delete-tables
  [node delete-from tables]
  (let [{:keys [using]} node]
    (cond->> tables
      true   (from-tables [delete-from])
      using  (from-tables using))))

(defn reduce-walk
  [f acc form]
  (let [pf (partial reduce-walk f)]
    (if (coll? form)
      (f (reduce pf acc form) form)
      (f acc form))))

(defn query-tables
  "Return all the tables in an sql statement."
  [query]
  (reduce-walk (fn [tables node]
                 (if (map? node)
                   (let [{:keys [from insert-into update delete-from]} node]
                     (cond->> tables
                       update      (from-join-tables node [update])
                       from        (from-join-tables node from)
                       insert-into (insert-tables insert-into)
                       delete-from (delete-tables node delete-from)))
                   tables))
               #{} query))

