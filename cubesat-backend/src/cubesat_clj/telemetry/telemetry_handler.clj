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


(defn- save-ttl-data
  "Process image fragment data sent by cubesat. Image comes over several fragments as
  rockblock only supports so much protocol. Image 'fragments' are then assembled into full images
  when fully collected, and saved into the image database"
  [ttl-data]
  (let [{:keys [image-serial-number image-fragment-number image-max-fragments image-data]} ttl-data]
    (save-cubesat-data ttl-data)
    (img/save-fragment image-serial-number image-fragment-number image-data)
    (img/try-save-image image-serial-number image-max-fragments)))


(defn- handle-cubesat-data
  "Handles a packet from the cubesat as per the Alpha specification. Reads an opcode,
  and depending on the result, parses and stores the corresponding data"
  [rockblock-report]
  (let [result (protocol/read-cubesat-data rockblock-report)
        operation (:telemetry-report-type result)]
    (case operation
      ::protocol/ttl (save-ttl-data result)
      (save-cubesat-data result))))


(defn handle-report!
  "Handles a report sent by the rockblock web service API, decodes from it the cubesat data,
  and routes it to cubesat data handlers."
  [rockblock-report]
  (println "Got Report:" "\r\n" rockblock-report "\r\n\r\n")
  (save-rockblock-report rockblock-report)
  (when (:data rockblock-report)
    (handle-cubesat-data rockblock-report))
  (http/ok))


(defn verify-rockblock-data
  "Middleware that verifies the JWT sent in a rockblock report, and extracts the data.
  Does not use data sent in report, but instead that which is decoded from the provided JWT since it is an exact copy."
  [handler]
  (fn [request]
    (if-let [verified-data (protocol/verify-rockblock-request (:body-params request))]
      (handler (assoc request :body-params verified-data))
      (http/unauthorized))))


(defn fix-rockblock-date
  "Middleware that fixes the date format in data from rockblock web services.
  Rockblock uses YY-MM-DD HH:mm:ss as the date format, despite their documentation claiming to use a more standard
  format: YYYY-MM-DDThh:mm:ssZ. This middleware takes the incoming format and converts it to YYYY-MM-DDThh:mm:ssZ
  in order to be consistent with rockblock's own documentation (and common sense).
  It does this by appending '20' to the start of the date string,
  which means this fix may not work after the year 2100."
  [handler]
  (fn [request]
    (let [time (get-in request [:body-params :transmit_time])
          formatted-time (str "20" (.replace time " " "T") "Z") ; Warning: This hack will cease to work in the year 2100
          fixed-request (assoc-in request [:body-params :transmit_time] formatted-time)]
      (println "middleware fixed time: " formatted-time)
      (handler fixed-request))))