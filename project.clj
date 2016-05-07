(defproject kirasystems/views-honeysql "0.1.3-SNAPSHOT"
  :description "HoneySQL view implementation for views"
  :url "https://github.com/kirasystems/views-honeysql"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :repositories [["releases" {:url "https://clojars.org/repo"
                              :sign-releases false
                              :username :env
                              :password :env}]]

  :dependencies [[kirasystems/views "1.4.8"]
                 [honeysql "0.6.3"]
                 [org.clojure/java.jdbc "0.5.8"]
                 [org.clojure/tools.logging "0.3.1"]]
  :plugins [[lein-ancient "0.6.10"]]
  :profiles {:dev {:dependencies
                   [[org.clojure/clojure "1.7.0"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
