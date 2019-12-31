(ns cubesat-clj.auth.user-store
  "For checking credentials and managing users in the user store file."
  (:require [cubesat-clj.config :as cfg]
            [clojure.edn :as edn]
            [buddy.hashers :as hash]))


(defn- get-user-data
  "Gets the user data from the user store file whose location is fetched from the config."
  [username]
  (let [config (cfg/get-config)
        users-file (cfg/users-file config)
        users (edn/read-string (slurp users-file))]
    (users username)))


(defn check-credentials
  "Checks username and password supplied. Returns true if valid, false if password is wrong or user doesn't exist."
  [username password]
  (let [hash (get-user-data username)]
    (and hash (hash/check password hash))))