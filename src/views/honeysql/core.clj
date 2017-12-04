(ns views.honeysql.core
  (:require
    [views.core :refer [hint]]
    [views.honeysql.util :refer [query-tables]]
    [honeysql.core :as hsql]
    [clojure.tools.logging :refer [error]]
    [clojure.java.jdbc :as j]))

(def send-hints! (atom (fn [hints] (error "send-hints! not configured"))))

(defmacro with-view-transaction
  "Like with-db-transaction, but sends view hints at end of transaction."
  [binding & forms]
  (let [tvar (first binding), db (second binding), args (drop 2 binding)]
    `(if (:hints ~db) ;; check if we are in a nested transaction
       (let [~tvar ~db] ~@forms)
       (let [hints#   (atom [])
             result#  (j/with-db-transaction [t# ~db ~@args]
                                             (let [~tvar (assoc ~db :hints hints#)]
                                               ~@forms))]
         (@send-hints! @hints#)
         result#))))

(defn execute-honeysql!
  "Always return keys for inserts."
  [db hsql-map]
  (if-let [table (:insert-into hsql-map)]
    (if (vector? table)
      (j/execute! db (hsql/format hsql-map))
      (apply j/insert! db table (:values hsql-map)))
    (j/execute! db (hsql/format hsql-map))))

(defn vexec!
  "Used to perform arbitrary insert/update/delete actions on the database,
   while ensuring that view hints are sent to the view system.
   Arguments are:
   - db: a clojure.java.jdbc database with optional :hints field.
    
     If hints field is present hints will be conj'd to the collection.
     Otherwise send-hints function set by set-hint-transport-fn! will
     be used.
  
   - action-map: the HoneySQL map for the insert/update/delete action"
  [db action-map]
  (let [results   (execute-honeysql! db action-map)
        hsql-hint (hint :views/honeysql (query-tables action-map))]
    (if-let [hints (:hints db)]
      (swap! hints conj hsql-hint)
      (@send-hints! [hsql-hint]))
    results))

(defn set-hint-transport-fn!
  "The hint transport function should take a collection of hints and
   send them to the configured view system. On a non-distribued server this
   can be: (fn [hints] (refresh-views! views-config hints))"
  [f]
  (reset! send-hints! f))
