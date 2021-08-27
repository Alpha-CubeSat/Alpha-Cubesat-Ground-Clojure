(ns cubesat-clj.telemetry.telemetry-handler
  "Handles requests from the rockblock web service containing data sent by the satellite,
  and processes that data."
  (:require [ring.util.http-response :as http]
            [cubesat-clj.databases.elasticsearch :as es]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.telemetry.cubesat-telemetry :as cs]
            [cubesat-clj.databases.image-database :as img]
            [cubesat-clj.telemetry.rockblock-telemetry :as rb]))

(defn- handle-cubesat-data
  "Handles a packet from the cubesat as per the Alpha specification. Reads an opcode,
  and depending on the result, parses and stores the corresponding data"
  [rockblock-report]
  (let [result (cs/read-cubesat-data rockblock-report)
        operation (:telemetry-report-type result)]
    (case operation
      ::cs/ttl (cs/save-ttl-data result)
      (cs/save-cubesat-data result))))

(defn handle-report!
  "Handles a report sent by the rockblock web service API, decodes from it the cubesat data,
  and routes it to cubesat data handlers."
  [rockblock-report]
  (println "Got Report:" "\r\n" rockblock-report "\r\n\r\n")
  (rb/save-rockblock-report rockblock-report)
  (when (:data rockblock-report)
    (handle-cubesat-data rockblock-report))
  (http/ok))

(defn verify-rockblock-data-mw
  "Middleware that verifies the JWT sent in a rockblock report, and extracts the data.
  Does not use data sent in report, but instead that which is decoded from the provided JWT since it is an exact copy."
  [handler]
  (fn [request]
    (if-let [verified-data (rb/verify-rockblock-request (:body-params request))]
      (handler (assoc request :body-params verified-data))
      (http/unauthorized))))

(defn fix-rockblock-date-mw
  "Middleware that fixes the date format in data from rockblock web services."
  [handler]
  (fn [request]
    (let [time (get-in request [:body-params :transmit_time])
          formatted-time (rb/fix-rb-datetime time)
          fixed-request (assoc-in request [:body-params :transmit_time] formatted-time)]
      (println "middleware fixed time: " formatted-time)
      (handler fixed-request))))