(defproject views/honeysql "0.1.1"
  :description "HoneySQL view implementation for views"
  :url "https://github.com/kirasystems/views-honeysql"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[views "1.4.1-SNAPSHOT"]
                 [honeysql "0.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.clojure/tools.logging "0.3.1"]]
  :plugins [[lein-ancient "0.6.7"]]
  :profiles {:dev {:dependencies
                   [[org.clojure/clojure "1.6.0"]]}})
