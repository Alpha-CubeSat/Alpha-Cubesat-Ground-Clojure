(ns cubesat-clj.telemetry.telemetry-handler
  "Handles requests from the rockblock web service containing data sent by the satellite,
  and processes that data."
  (:require [ring.util.http-response :as http]
            [cubesat-clj.databases.elasticsearch :as es]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.telemetry.telemetry-protocol :as protocol]
            [cubesat-clj.databases.image-database :as img]))


(defn- save-rockblock-report
  "Saves a rockblock report to elasticsearch. Gets latitude and longitude
   data and makes a single field out of them"
  [data]
  (let [lat (:iridium_latitude data)
        lon (:iridium_longitude data)
        location {:lat lat, :lon lon}
        result (assoc data :location location)
        index (cfg/rockblock-db-index (cfg/get-config))]
    (es/index! index es/daily-index-strategy result)))


(defn- save-cubesat-data
  "Saves a cubesat report to elasticsearch"
  [data]
  (let [index (cfg/cubesat-db-index (cfg/get-config))]
    (es/index! index es/daily-index-strategy data)))


(defn- handle-bad-data
  "Process a message with an unknown opcode or misformed data by saving the exception in elasticsearch"
  [{:keys [transmit_time imei data]} packet exception-str]
  (let [report (assoc {} :imei imei :transmit-time transmit_time :error exception-str :raw-data data)]
    (save-cubesat-data report)))


(defn- handle-no-data
  "Process empty respnose from cubesat by just logging that it happened into into ES"
  [{:keys [transmit_time imei]} packet]
  (let [report (assoc {} :imei imei :transmit-time transmit_time)]
    (save-cubesat-data report)))


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
    (try (case op
           ::protocol/empty-packet (handle-no-data rockblock-report packet)
           ::protocol/imu (handle-imu-data rockblock-report packet)
           ::protocol/ttl (handle-ttl-data rockblock-report packet))
         (catch Exception e (handle-bad-data rockblock-report packet (.getMessage e))))))


;; TODO Verifying the JWT and replacing original request with its contents should really be taken care of by middleware
(defn handle-report!
  "Handles a report sent by the rockblock web service API.
  Does not use data sent in report, but instead that which is decoded from
  the provided JWT"
  [rockblock-report]
  (if-let [report-data (assoc (protocol/verify-rockblock-request rockblock-report)
                         :transmit_time (:fixed-transmit-time rockblock-report))]
    (do (println "Got Report:" "\r\n" report-data "\r\n\r\n")
        (save-rockblock-report report-data)
        (when (:data report-data)
          (handle-cubesat-data report-data))
        (http/ok))
    (http/unauthorized)))


(defn fix-rockblock-date
  "Middleware that fixes the date format in data from rockblock web services.
  Rockblock uses YY-MM-DD HH:mm:ss as the date format, despite their documentation claiming to use a more standard
  format: YYYY-MM-DDThh:mm:ssZ. This middleware takes the incoming format and converts it to YYYY-MM-DDThh:mm:ssZ
  in order to be consistent with rockblock's documentation. It does this by appending '20' to the start of the date
  string, which means this fix may not work after the year 2100."
  [handler]
  (fn [request]
    (let [time (get-in request [:body-params :transmit_time])
          formatted-time (str "20" (.replace time " " "T") "Z") ; Warning: This hack will cease to work in the year 2100
          fixed-request (assoc-in request [:body-params :fixed-transmit-time] formatted-time)
          fixed-request-2 (assoc-in fixed-request [:body-params :transmit_time] formatted-time)]
      (println "middleware fixed time: " formatted-time)
      (handler fixed-request-2))))