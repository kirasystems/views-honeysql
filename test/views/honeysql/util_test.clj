(ns views.honeysql.util-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [honeysql.core :as hsql]
            [views.honeysql.util :as util]))

(deftest query-tables-test
  (testing "will return the list of tables in a recursive query"
    (let [query (hsql/build
                  {:with-recursive [[:test_query {:select [:foo] :from [:bar]}]]})]
      (is (= #{:bar}
             (util/query-tables query))))))
