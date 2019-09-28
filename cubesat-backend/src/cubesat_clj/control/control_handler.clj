(ns cubesat-clj.control.control-handler
  (:require [cubesat-clj.control.control-protocol :as protocol]
            [cubesat-clj.config :as config]
            [ring.util.http-response :as http]))


(defn handle-command!
  "Handles a command sent to ground system for the satellite.
  Authenticates with rockblock web services using credentials
  provided in the config file"
  [command]
  (let [str-cmd (protocol/parse-command-args command)
        {[user pass] :basic-auth imei :imei} (-> (config/get-config) :control :rockblock)]
    (protocol/send-uplink imei user pass str-cmd)
    (http/ok {:response "command sent?"})))