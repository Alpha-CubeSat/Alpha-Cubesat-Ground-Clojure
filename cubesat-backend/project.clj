(defproject cubesat-clj "0.1.0-SNAPSHOT"
  :description "Backend Ground Station software for the AlphaCubesat project"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 [org.clojure/core.match "0.3.0"]
                 [clojurewerkz/elastisch "3.0.1"]
                 [aero "1.1.3"]
                 [buddy/buddy-auth "2.2.0"]
                 [clj-http "3.10.0"]
                 [buddy/buddy-hashers "1.4.0"]]
  :ring {:handler cubesat-clj.handler/app}
  :uberjar-name "cubesat.jar"
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]
                   :plugins      [[lein-ring "0.12.5"]
                                  [lein-codox "0.10.7"]]}
             :uberjar {:aot :all}})
