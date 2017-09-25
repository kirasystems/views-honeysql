(ns views.honeysql.util-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [honeysql.core :as hsql]
            [views.honeysql.util :as util]))

(deftest query-tables-test
  (testing "will return the list of tables in a simple select from
  table uery"
    (let [query (hsql/build {:select [:field1]
                             :from [:table1]})]
      (is (= #{:table1}
             (util/query-tables query)))))
  (testing "will return the list of tables in a query containing a sub query"
    (let [query (hsql/build {:with [[:sub_query {:select [:field2]
                                                 :from [:table2]}]]
                             :select [:field1]
                             :from [:table1]})]
      (is (= #{:table1 :table2}
             (util/query-tables query)))))
  (testing "will retun the list of tables used in a sub queries from a
  compond query using set operation"
    (testing "intersect"
      (let [query (hsql/build {:intersect [{:select [:field1]
                                            :from [:table1]}
                                           {:select [:field2]
                                            :from [:table2]}]})]
        (is (= #{:table1 :table2}
               (util/query-tables query)))))
    (testing "union"
      (let [query (hsql/build {:union [{:select [:field1]
                                        :from [:table1]}
                                       {:select [:field2]
                                        :from [:table2]}]})]
        (is (= #{:table1 :table2}
               (util/query-tables query)))))
    (testing "union all"
      (let [query (hsql/build {:union-all [{:select [:field1]
                                            :from [:table1]}
                                           {:select [:field2]
                                            :from [:table2]}]})]
        (is (= #{:table1 :table2}
               (util/query-tables query)))))
    (testing "intersect as part of a sub query"
      (let [query (hsql/build {:with [[:sub_query {:intersect [{:select [:field2]
                                                                :from [:table2]}
                                                               {:select [:field3]
                                                                :from [:table3]}]}]]
                               :select [:field1]
                               :from [:table1 :table3]})]
        (is (= #{:table1 :table2 :table3}
               (util/query-tables query))))))
  (testing "will return the list of tables in a recursive query"
    (let [query (hsql/build
                 {:with-recursive [[:test_query {:select [:foo] :from [:bar]}]]})]
      (is (= #{:bar}
             (util/query-tables query))))))
