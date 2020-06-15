(ns views.honeysql.util-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [honeysql.core :as hsql]
            [views.honeysql.util :as util]))

(deftest query-tables-select-test
  (testing "will return the list of tables in a simple select from
  table query"
    (let [query (hsql/build {:select [:field1]
                             :from [:table1]})]
      (is (= #{:table1}
             (util/query-tables query)))))
  (testing "will return the list of tables in a query containing a subquery"
    (let [query (hsql/build {:with [[:sub_query {:select [:field2]
                                                 :from [:table2]}]]
                             :select [:field1]
                             :from [:table1]})]
      (is (= #{:table1 :table2}
             (util/query-tables query)))))
  (testing "will return the list of tables used in subqueries from a
  compound query using set operation"
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
    (testing "intersect as part of a subquery"
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
             (util/query-tables query)))))

  (testing "will return table in exists"
    (let [in-select-query {:select [[{:exists {:select [1] :from [:foo]}} :col]] :from [:bar]}]
      (is (= #{:foo :bar}
             (util/query-tables in-select-query))))
    (let [in-where-query {:select [:col] :from [:bar] :where [:exists {:select [1] :from [:foo]}]}]
      (is (= #{:foo :bar}
             (util/query-tables in-where-query)))))
  (testing "will return table mentioned in select clause"
    (let [query {:select [[{:select [1] :from [:foo] :limit 1} :col]] :from [:bar]}]
      (is (= #{:foo :bar}
             (util/query-tables query)))))

  (testing "exist select"
    (let [query {:exists {:select [:a] :from [:foo]}}]
      (is (= #{:foo} (util/query-tables query)))))

  (testing "multiple from,join"
    (let [query {:select [[{:select [:%count.aa.a]
                            :from [[:AA :aa]
                                   [:BB :bb]
                                   [:CC :cc]]} :ca]
                          [{:select [:%count.dd.a]
                            :from {:DD :dd}} :cd]]
                 :from {:EE :ee}
                 :join [[:FF :ff] [:= :ff.a :ee.a]
                        [:GG :gg] [:= :gg.a :ee.a]]}]
      (is (= #{:AA :BB :CC :DD :EE :FF :GG}
             (util/query-tables query)))))

  (testing "DEV-9832, query-tables should pick tables in filter clause"
    (let [[fid uid] [1 1]
          rdcs (hsql/build :select [:dr.did]
                           :from   [[:DW :dw]]
                           :join   [[:WF :wf] [:= :wf.wid :dw.wid]
                                    [:DD :d]  [:= :d.did :dw.did]
                                    [:DR :dr] [:= :dr.did :d.did]]
                           :where  [:and
                                    [:= :wf.fid fid]
                                    [:= :d.sd false]])
          cdcs (hsql/build :select [:ods.did]
                           :from   [[:ODS :ods]]
                           :join   [[:OMM :omm] [:= :ods.batch_id :omm.batch_id]]
                           :where  [:and
                                    [:= :omm.fid fid]
                                    [:= :omm.ic true]
                                    [:= :omm.sd false]])
          query             {:select    [:fo.option
                                         :fo.pos
                                         [(hsql/call :filter
                                                     :%count-distinct.fia.did
                                                     [:in :fia.did (hsql/build :union [rdcs cdcs])])
                                          :answer_documents]]
                             :from      [[:FO :fo]]
                             :left-join [[:FIA :fia] [:= :fia.foid :fo.foid]]
                             :where     [:and
                                         [:= :fo.fid fid]
                                         [:= :fo.fot "standard"]]
                             :group-by  [:fo.option :fo.pos]}]
      (is (= #{:DR
               :DD
               :DW
               :FIA
               :FO
               :ODS
               :OMM
               :WF}
             (util/query-tables query))))))

(deftest query-tables-delete-test
  (testing "simple delete"
    (let [query (hsql/build {:delete-from :foo
                             :where [:= :id :name]})]
      (is (= #{:foo} (util/query-tables query))))

    (let [query (hsql/build {:delete-from [:foo :f]
                             :where [:= :f.id :f.name]})]
      (is (= #{:foo} (util/query-tables query)))))

  (testing "delete with using"
    (let [query {:delete-from :foo
                   :using [:bar]
                   :where [:= :foo.id :bar.id]}]
      (is (= #{:foo :bar} (util/query-tables query))))

    (let [query {:delete-from [:foo :f]
                 :using {:bar :b}
                 :where [:= :f.id :b.id]}]
      (is (= #{:foo :bar} (util/query-tables query))))

    (let [query {:delete-from [:foo :f]
                 :using [[:bar :b]]
                 :where [:= :f.id :b.id]}]
      (is (= #{:foo :bar} (util/query-tables query)))))

  (testing "delete in with"
    (let [query (hsql/build :with [[:DELETED
                                    {:delete-from [:foo :f]
                                     :where       [:= :f.id :f.name]
                                     :returning   [:f.id :f.name]}]]
                            :insert-into
                            [[:bar [:id :name]]
                             {:select [:*]
                              :from   [:DELETED]}])]
      (is (= #{:foo :bar :DELETED} (util/query-tables query)))))

  (testing "delete with using in with"
    (let [query (hsql/build :with [[:DELETED
                                    {:delete-from [:foo :f]
                                     :using {:baz :z}
                                     :where       [:= :f.id :z.id]
                                     :returning   [:f.id :f.name]}]]
                            :insert-into
                            [[:bar [:id :name]]
                             {:select [:*]
                              :from   [:DELETED]}])]
      (is (= #{:foo :bar :baz :DELETED} (util/query-tables query))))))

(deftest query-tables-insert-test
  (testing "insert"
    (let [query (hsql/build :insert-into :foo
                            :values [{:id 1
                                      :val 2}])]
      (is (= #{:foo} (util/query-tables query)))))

  (testing "insert into select"
    (let [query (hsql/build :insert-into
                            [[:foo [:id :name]]
                             {:select [:id :name]
                              :from   [:bar]}])]
      (is (= #{:foo :bar} (util/query-tables query))))

    (let [query (hsql/build :insert-into
                            [:foo {:select [:id]
                                   :from [:bar]}])]
      (is (= #{:foo :bar} (util/query-tables query))))))

(deftest query-tables-update-test
  (testing "update"
    (let [query (hsql/build {:update :foo
                             :set    {:a 1}
                             :where  [:= :b 42]})]
      (is (= #{:foo} (util/query-tables query)))))

  (testing "update with join"
    (let [query (hsql/build {:update :foo
                             :join   [:bar [:= :bar.id :foo.bar_id]]
                             :set    {:a 1}
                             :where  [:= :bar.b 42]})]
      (is (= #{:foo :bar} (util/query-tables query)))))

  (testing "update and from are in same time"
    (let [query (hsql/build {:update [:foo :f]
                             :set    {:kind :c.test}
                             :from   [[{:select [:b.test]
                                        :from   [[:bar :b]]
                                        :where  [:= :b.id 1]} :c]]
                             :where  [:= :f.kind "drama"]})]
      (is (= #{:foo :bar} (util/query-tables query))))))

