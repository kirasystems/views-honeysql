(ns views.honeysql.view
  (:require
    [views.protocols :refer :all]
    [views.honeysql.util :refer [query-tables]]
    [honeysql.core :as hsql]
    [clojure.set :refer [intersection]]
    [clojure.java.jdbc :as j]
    [clojure.tools.logging :refer [warn]]))

(defrecord HSQLView [id db query-fn post-fn]
  IView
  (id [_] id)
  (data [_ namespace parameters]
    (let [start (System/currentTimeMillis)
          data  (j/query db (hsql/format (apply query-fn parameters)) :row-fn post-fn)
          time  (- (System/currentTimeMillis) start)]
      (when (>= time 1000) (warn id "took" time "msecs"))
      data))
  (relevant? [_ namespace parameters hints]
    (let [tables (query-tables (apply query-fn parameters))
          nhints (filter #(= :views/honeysql (:namespace %)) hints)]
      (boolean (some #(not-empty (intersection (:hint %) tables)) nhints)))))

(defn view
  "Creates a Honey SQL view that uses a jdbc database configuration"
  ([id db hsql-fn post-fn] (HSQLView. id db hsql-fn post-fn))
  ([id db hsql-fn] (view id db hsql-fn identity)))
