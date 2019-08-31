(ns cubesat-clj.telemetry.telemetry-handler
  "Handles requests from the rockblock web service containing data sent by the satellite,
  and processes that data."
  (:require [ring.util.http-response :as http]
            [cubesat-clj.databases.elasticsearch :as es]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.telemetry.telemetry-protocol :as protocol]))

(defn db-indices
  "Gets the elasticsearch indices from the telemetry configuration"
  []
  (-> (cfg/get-config) :telemetry :elasticsearch-indices))

(defn handle-report!
  "Handles a report sent by the rockblock web service API.
  Does not use data sent in report, but instead that which is decoded from
  the provided JWT"
  [rockblock-report]
  (if-let [report-data (protocol/verify-rockblock-request rockblock-report)]
    (do (println "Got Report:" "\r\n" report-data "\r\n\r\n")
        (es/index! (:rockblock (db-indices)) es/daily-index-strategy report-data)
        (http/ok))
    (http/unauthorized)))