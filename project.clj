(defproject net.kronkltd/octohipster "0.3.0-SNAPSHOT"
  :description "A hypermedia REST HTTP API library for Clojure"
  :url "https://github.com/duck1123/octohipster"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ring/ring-core "1.5.0"]
                 [ring.middleware.jsonp "0.1.6"]
                 [liberator "0.14.1"
                  :exclusions [hiccup]]
                 [clout "2.1.2"]
                 [cheshire "5.6.3"]
                 [compojure "1.5.1"]
                 [hiccup "1.0.5"]
                 [clj-yaml "0.4.0"]
                 [inflections "0.12.2"]
                 [org.bovinegenius/exploding-fish "0.3.4"]
                 [com.github.fge/json-schema-validator "2.2.6"]
                 [com.damnhandy/handy-uri-templates "2.1.6"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"                  :exclusions [org.clojure/clojure]]
                                  [ring-mock "0.1.5"              :exclusions [org.clojure/clojure]]
                                  [org.slf4j/slf4j-log4j12 "1.7.21"]
                                  ;; [org.apache.logging.log4j/log4j-core "2.7"]
                                  ]}}
  :plugins [[cider/cider-nrepl "0.10.0-SNAPSHOT"]
            [codox "0.8.10"]
            [lein-ancient "0.6.7"]
            [lein-release "1.1.3"]
            [lein-midje     "3.1.3"]]
  :lein-release {:deploy-via :lein-deploy}
  :auto-clean false
  :repositories [["snapshots" {:url "http://repo.jiksnu.org/repository/maven-snapshots/"
                               :username [:gpg :env/repo_username]
                               :password [:gpg :env/repo_password]}]
                 ["releases" {:url "http://repo.jiksnu.org/repository/maven-releases/"
                              :username [:gpg :env/repo_username]
                              :password [:gpg :env/repo_password]}]]
  :jar-exclusions [#"example.clj"]
  :codox {:exclude example
          :src-dir-uri "https://github.com/duck1123/octohipster/blob/master"
          :src-linenum-anchor-prefix "L"}
  )
