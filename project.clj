(defproject donald-tweets "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [twitter-api "0.7.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-simple "1.6.6"]
                 [clj-http "2.3.0"]
                 [environ "1.0.0"]]
  :plugins [[lein-environ "1.0.0"]]               
  :uberjar-name "donald-tweets-standalone.jar"
  :main donald-tweets.core)
