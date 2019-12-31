(ns cubesat-clitools.core
  (:require [buddy.hashers :as hash]
            [clojure.edn :as edn])
  (:import (java.io Console File)
           (java.util Scanner))
  (:gen-class))

(def console (System/console))

(defn exit
  []
  (println "Enter any key to exit...")
  (read-line))

(defn create-user
  [user-file]
  (let [_ (println "Username: ")
        username (read-line)
        _ (println "Password: ")
        password (String. (.readPassword console))
        hash (hash/derive password)
        user-db-file (File. ^String user-file)
        _ (.createNewFile user-db-file)                     ;; Will not actually create a new file if it already exists
        user-db (edn/read-string (slurp user-db-file))
        updated (assoc user-db username hash)
        _ (spit user-db-file updated)]
    (println "User created: " username hash)
    (exit)))

(defn delete-user
  []
  (exit))

(defn -main
  "Command line interface for editing users/config for cubesat backend. Accepts name of users file as argument."
  [& args]
  (if (< (count args) 1)
    (do (println "Must supply user filename as argument")
        (exit))
    (do
      (println "Cubesat backend CLI tools...")
      (println "Options:")
      (println "1 Create Control User")
      (println "2 Remove Control User")
      (let [user-file (first args)
            input (read-line)]
        (cond
          (= input "1") (create-user user-file)
          (= input "2") (delete-user)
          :else (exit))))))
