(ns cubesat-clj.telemetry.telemetry-handler
  "Handles requests from the rockblock web service containing data sent by the satellite,
  and processes that data."
  (:require [ring.util.http-response :as http]
            [cubesat-clj.databases.elasticsearch :as es]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.telemetry.telemetry-protocol :as protocol]))


(defn- db-indices
  "Gets the elasticsearch indices from the telemetry configuration"
  []
  (-> (cfg/get-config) :telemetry :elasticsearch-indices))

(defn- save-rockblock-report
  "Saves a rockblock report to elasticsearch"
  [rockblock-report]
  (es/index! (:rockblock (db-indices)) es/daily-index-strategy report-data))


(defn- handle-imu-data
  "Processes IMU data from a cubesat packet by saving the report to Elasticsearch
  with some metadata from the rockblock data"
  [rockblock-report packet]
  (let [imu-data (protocol/read-imu-data packet)
        timestamp (:at rockblock-report)
        transmit-time (:transmit_time rockblock-report)
        imei (:imei rockblock-report)
        report (assoc imu-data :imei imei
                               :timestamp timestamp
                               :transmit-time transmit-time)]
    (es/index! (:cubesat db-indices) es/daily-index-strategy report)))


(defn- handle-cubesat-data
  "Handles a packet from the cubesat as per the Alpha specification. Reads an opcode,
  and depending on the result, parses and stores the corresponding data"
  [rockblock-report]
  (let [packet (protocol/get-cubesat-message-binary rockblock-report)
        op (protocol/read-opcode packet)]
    (case op
      ::protocol/imu (handle-imu-data rockblock-report packet))))


(defn handle-report!
  "Handles a report sent by the rockblock web service API.
  Does not use data sent in report, but instead that which is decoded from
  the provided JWT"
  [rockblock-report]
  (if-let [report-data (protocol/verify-rockblock-request rockblock-report)]
    (do (println "Got Report:" "\r\n" report-data "\r\n\r\n")
        (save-rockblock-report report-data)
        (handle-cubesat-data report-data)
        (http/ok))
    (http/unauthorized)))