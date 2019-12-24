(ns cubesat-clj.control.control-handler
  "Handles requests to control the satellite."
  (:require [cubesat-clj.control.control-protocol :as protocol]
            [cubesat-clj.config :as config]
            [ring.util.http-response :as http]))


(defn handle-command!
  "Handles a command sent to ground system for the satellite.
  Authenticates with rockblock web services using credentials
  provided in the config file"
  [command]
  (let [str-cmd (protocol/parse-command-args command)
        config (config/get-config)
        [user pass] (config/rockblock-credentials config)
        imei (config/rockblock-imei config)]
    (protocol/send-uplink imei user pass str-cmd)
    (http/ok {:response "command sent?"})))