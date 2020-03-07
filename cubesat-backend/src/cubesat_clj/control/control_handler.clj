(ns cubesat-clj.control.control-handler
  "Handles requests to control the satellite."
  (:require [cubesat-clj.control.control-protocol :as protocol]
            [cubesat-clj.config :as config]
            [cubesat-clj.databases.elasticsearch :as db]
            [ring.util.http-response :as http]))

(defn- log-command
  "Stores the result of submitting a command into ElasticSearch"
  [imei raw-request parsed-string rockblock-response]
  (let [config (config/get-config)
        index (config/control-db-index config)
        log {:imei imei
             :command raw-request
             :parsed parsed-string
             :result rockblock-response}]
    (db/index! index db/daily-index-strategy log)))

(defn handle-command!
  "Handles a command sent to ground system for the satellite.
  Authenticates with rockblock web services using credentials
  provided in the config file"
  [command]
  (let [str-cmd (protocol/parse-command-args command)
        config (config/get-config)
        [user pass] (config/rockblock-credentials config)
        imei (config/rockblock-imei config)
        rockblock-response (protocol/send-uplink imei user pass str-cmd)]
    (log-command imei command str-cmd rockblock-response)
    (http/ok {:response rockblock-response})))