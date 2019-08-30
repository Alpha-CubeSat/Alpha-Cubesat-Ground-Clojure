(ns cubesat-clj.telemetry.telemetry-handler
  "Handles requests from the rockblock web service containing data sent by the satellite,
  and processes that data."
  (:require [ring.util.http-response :as http]
            [cubesat-clj.databases.elasticsearch :as es]
            [cubesat-clj.telemetry.telemetry-config :as cfg]))

(defn db-indices
  "Gets the elasticsearch indices from the telemetry configuration"
  []
  (:elasticsearch-indices (cfg/get-config)))

(defn handle-report!
  "Handles a report sent by the rockblock web service API"
  [rockblock-report]
  (let [content (:message rockblock-report)
        timestamp (:at rockblock-report)]
    (println "Got Report:" "\r\n" rockblock-report "\r\n\r\n")
    (es/index! (:rockblock (db-indices)) es/daily-index-strategy rockblock-report)
    (http/ok)))