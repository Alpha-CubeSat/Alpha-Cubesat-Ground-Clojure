(defproject cubesat-clitools "0.1.0-SNAPSHOT"
  :description "CLI tools for cubesat-backend. Allows for control user management and configuration editing."
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [buddy/buddy-hashers "1.4.0"]]
  :repl-options {:init-ns cubesat-clitools.core}
  :uberjar-name "cubesat-utils.jar"
  :main cubesat-clitools.core)
