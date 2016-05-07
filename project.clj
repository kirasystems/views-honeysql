(defproject kirasystems/views-honeysql "0.1.2"
  :description "HoneySQL view implementation for views"
  :url "https://github.com/kirasystems/views-honeysql"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :repositories [["releases" {:url "https://clojars.org/repo"
                              :sign-releases false
                              :username :env
                              :password :env}]]

  :dependencies [[views "1.4.4"]
                 [honeysql "0.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.clojure/tools.logging "0.3.1"]]
  :plugins [[lein-ancient "0.6.7"]]
  :profiles {:dev {:dependencies
                   [[org.clojure/clojure "1.6.0"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
