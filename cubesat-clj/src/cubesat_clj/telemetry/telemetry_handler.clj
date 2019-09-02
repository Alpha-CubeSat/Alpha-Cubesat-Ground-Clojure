(ns cubesat-clj.telemetry.telemetry-handler
  "Handles requests from the rockblock web service containing data sent by the satellite,
  and processes that data."
  (:require [ring.util.http-response :as http]
            [cubesat-clj.databases.elasticsearch :as es]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.telemetry.telemetry-protocol :as protocol]
            [cubesat-clj.databases.image-database :as img]))


(defn- db-indices
  "Gets the elasticsearch indices from the telemetry configuration"
  []
  (-> (cfg/get-config) :telemetry :elasticsearch-indices))


(defn- save-rockblock-report
  "Saves a rockblock report to elasticsearch. Gets latitude and longitude
   data and makes a single field out of them"
  [data]
  (let [lat (:iridium_latitude data)
        lon (:iridium_longitude data)
        location {:lat lat, :lon lon}
        result (assoc data :location location)]
    (es/index! (:rockblock (db-indices)) es/daily-index-strategy result)))


(defn- save-cubesat-data
  "Saves a cubesat report to elasticsearch"
  [data]
  (es/index! (:cubesat (db-indices)) es/daily-index-strategy data))


(defn- handle-ttl-data
  "Process image fragment data sent by cubesat. Image comes over several fragments as
  rockblock only supports so much protocol. Image 'fragments' are then assembled into full images
  when fully collected, and saved into the image database"
  [{:keys [transmit_time imei]} packet]
  (let [image-data (protocol/read-image-data packet)
        {:keys [image-serial-number image-fragment-number image-max-fragments image-data]} image-data
        report (assoc image-data :imei imei :transmit-time transmit_time)]
    (save-cubesat-data report)
    (img/save-fragment image-serial-number image-fragment-number image-data)
    (img/try-save-image image-serial-number image-max-fragments)))


(defn- handle-imu-data
  "Processes IMU data from a cubesat packet by saving the report to Elasticsearch
  with some metadata from the rockblock data"
  [{:keys [transmit_time imei]} packet]
  (let [imu-data (protocol/read-imu-data packet)
        report (assoc imu-data :imei imei :transmit-time transmit_time)]
    (save-cubesat-data report)))


(defn- handle-cubesat-data
  "Handles a packet from the cubesat as per the Alpha specification. Reads an opcode,
  and depending on the result, parses and stores the corresponding data"
  [rockblock-report]
  (let [packet (protocol/get-cubesat-message-binary rockblock-report)
        op (protocol/read-opcode packet)]
    (case op
      ::protocol/imu (handle-imu-data rockblock-report packet)
      ::protocol/ttl (handle-ttl-data rockblock-report packet))))


(defn handle-report!
  "Handles a report sent by the rockblock web service API.
  Does not use data sent in report, but instead that which is decoded from
  the provided JWT"
  [rockblock-report]
  (if-let [report-data (protocol/verify-rockblock-request rockblock-report)]
    (do (println "Got Report:" "\r\n" report-data "\r\n\r\n")
        (save-rockblock-report report-data)
        (when (:data report-data)
          (handle-cubesat-data report-data))
        (http/ok))
    (http/unauthorized)))