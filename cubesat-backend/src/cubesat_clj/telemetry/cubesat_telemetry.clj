(ns cubesat-clj.telemetry.cubesat-telemetry
  "Specifies/parses formats used by the RockBlock API and cubesat.
  These formats are described in the RockBlock web services docs at:
  https://docs.rock7.com/reference#push-api
  And for the satellite in the Google Drive for Alpha"
  (:require [schema.core :as s]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [buddy.core.keys :as keys]
            [cheshire.core :as json]
            [clojure.string :as str]
            [cubesat-clj.util.binary.byte-buffer :as buffer]
            [cubesat-clj.util.binary.binary-reader :as reader]
            [cubesat-clj.util.binary.hex-string :as hex]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.databases.elasticsearch :as es]
            [cubesat-clj.databases.image-database :as img]
            [cubesat-clj.telemetry.rockblock-telemetry :as rb]))

(def ^:const opcodes
  "Packet opcodes for cubesat (see Alpha documentation for specification)"
  {21  ::normal-report
   42  ::ttl})

(defn save-cubesat-data
  "Saves a cubesat report to elasticsearch"
  [data]
  (let [index (cfg/cubesat-db-index (cfg/get-config))]
    (es/index! index es/daily-index-strategy data)))

(defn save-image-data
  "Saves an image fragment report to elasticsearch"
  [data]
  (let [index (cfg/image-db-index (cfg/get-config))]
    (es/index! index es/daily-index-strategy data)))

(defn save-ttl-data
  "Process image fragment data sent by cubesat. Image comes over several fragments as
  rockblock only supports so much protocol. Image 'fragments' are then assembled into full images
  when fully collected, and saved into the image database"
  [ttl-data]
  (let [{:keys [imei transmit-time serial-number fragment-number fragment-data max-fragments]} ttl-data
        meta {:imei imei :transmit-time transmit-time}]
    (img/save-fragment serial-number fragment-number fragment-data)
    (img/try-save-image serial-number max-fragments)
    (let [data (img/get-img-display-info)]
      (save-image-data (merge meta data)))))

(defn- map-range
  "Recreation of Arduino map() function used in flight code in order to convert imu data to correct imu values.
  https://www.arduino.cc/reference/en/language/functions/math/map/"
  [x in-min in-max out-min out-max]
  (+ out-min
     (* (/ (- out-max out-min)
           (- in-max in-min))
        (- x in-min))))

(defn- compute-normal-report-values
  [{:keys [altitude longitude latitude downlink-period] :as cubesat-data}]
  (assoc cubesat-data
         :altitude (map-range (float altitude) 0 255 0 35000)
         :longitude (map-range (float longitude) 0 255 -77.6088 -75.9180)
         :latitude (map-range (float latitude) 0 255 42.0987 43.156)
         :downlink-period (map-range (float downlink-period) 0 255 1000 10000)))

(defn- read-opcode
  "Reads the opcode of an incoming packet. If empty packet is received, returns ::empty-packet instead"
  [packet]
  (if (= (reader/remaining packet) 0)
    ::empty-packet
    (-> packet
        (reader/read-uint8)
        opcodes)))

(defn- read-hex-fragment
  "Reads the hexadecimal string of an image fragment to determine if the 
   fragment is the last fragment, which is indicated by the end-marker 'ffd9'. 
   Returns the serial number in decimal form, the fragment number in 
   decimal form, the max number of fragments in decimal form, and the hex string 
   of fragment data needed to be read (minus the opcode, image serial number and 
   fragment number).
   
   If the image fragment is not last, then the max number of fragments is set 
   arbitrarilty to 100, and the entirety of the fragment portion in hex string 
   must be read. If the image fragment is the last, then the max number of 
   fragments is set to (last fragment number + 1), and the hexadecimal string is 
   read up to 'ffd9'.
   
   Notes: - The serial number is stored in the indices of [2, 4) of the hex 
            string.
          - The fragment number is stored in the indices [10, 12) of the hex 
            string. Fragment count starts at 0.
          - The fragment data starts at index 12 and goes to the end of of the
            hex-string or to the end of end-marker 'ffd9'.
          - The entire hex string for an image fragment data is 70 bytes long 
            (140 characters)."
  [rockblock-data]
  (if (str/includes? rockblock-data "ffd9") ;change to "FFD9" for test2.clj
    (let [serial-number (Integer/parseInt (subs rockblock-data 2 4) 16)
          fragment-number (Integer/parseInt (subs rockblock-data 10 12) 16)
          max-fragments (+ 1 (Integer/parseInt (subs rockblock-data 10 12) 16))
          end-boundary (+ 4 (str/index-of rockblock-data "ffd9")) ;change to "FFD9" for test2.clj
          fragment-data (hex/hex-str-to-bytes (subs rockblock-data 12 end-boundary))]
      {:serial-number serial-number, :fragment-number fragment-number, :max-fragments max-fragments, :fragment-data fragment-data})
    (let [serial-number (Integer/parseInt (subs rockblock-data 2 4) 16)
          fragment-number (Integer/parseInt (subs rockblock-data 10 12) 16)
          max-fragments 100
          fragment-data (hex/hex-str-to-bytes (subs rockblock-data 12 140))]
      {:serial-number serial-number, :fragment-number fragment-number, :max-fragments max-fragments, :fragment-data fragment-data})))

(defmulti read-packet-data
  "Reads data from a packet based on opcode.
          Note: Image data is received in fragments, :data-length bytes each, which must be assembled into a full image
          after receiving all fragments. The serial number is which image is being sent, and the fragment number
          is which part of the image being sent"
  (fn [[opcode packet]] opcode))

(defmethod read-packet-data ::ttl
  [[_ rockblock-data]]
  (read-hex-fragment rockblock-data))

(defmethod read-packet-data ::normal-report
  [[_ packet]]
  (-> packet
      (reader/read-structure
       [:is-photoresistor-covered ::reader/uint8
        :is-door-button-pressed ::reader/uint8
        :mission-mode ::reader/uint8
        :fire-burnwire ::reader/uint8
        :arm-burnwire ::reader/uint8
        :burnwire-burn-time ::reader/uint8
        :burnwire-armed-timeout-limit ::reader/uint8
        :burnwire-mode ::reader/uint8
        :burnwire-attempts ::reader/uint8
        :downlink-period ::reader/uint8
        :waiting-messages ::reader/uint8
        :is-command-waiting ::reader/uint8
        :x-mag ::reader/uint8
        :y-mag ::reader/uint8
        :z-mag ::reader/uint8
        :x-gyro ::reader/uint8
        :y-gyro ::reader/uint8
        :z-gyro ::reader/uint8
        :temp ::reader/uint8
        :temp-mode ::reader/uint8
        :solar-current ::reader/uint8
        :in-sun ::reader/uint8
        :acs-mode ::reader/uint8
        :battery-voltage ::reader/uint8
        :fault-mode ::reader/uint8
        :check-x-mag ::reader/uint8
        :check-y-mag ::reader/uint8
        :check-z-mag ::reader/uint8
        :check-x-gyro ::reader/uint8
        :check-y-gyro ::reader/uint8
        :check-z-gyro ::reader/uint8
        :check-temp ::reader/uint8
        :check-solar-current ::reader/uint8
        :check-battery ::reader/uint8
        :take-photo ::reader/uint8
        :camera-on ::reader/uint8])))

(defn- report-metadata
  "Returns rockblock metadata such as transmit time from a rockblock report as a map"
  [rockblock-report]
  (let [{:keys [imei transmit_time]} rockblock-report]
    {:imei          imei
     :transmit-time transmit_time}))

(defn- error-data
  "Returns a map containing a cubesat report with the error opcode, raw data, and an error message"
  [rockblock-report error-msg]
  {:telemetry-report-type ::error
   :error    error-msg
   :raw-data (:data rockblock-report)})

(defn read-cubesat-data
  "Reads the cubesat data inside a rockblock report. Returns a map containing the data if read, or an error report
  if the packet is empty or some issue occurred. If the data is successfully read, the opcode is returned in the
  result map as :telemetry-report-type. Otherwise :telemetry-report-type is set to ::error"
  [rockblock-report]
  (let [packet (rb/get-cubesat-message-binary rockblock-report)
        op (read-opcode packet)
        meta (report-metadata rockblock-report)
        result (if (= op ::empty-packet)
                 (error-data rockblock-report "empty packet")
                 (try
                   (if (= op ::ttl)
                     (assoc (read-packet-data [op (:data rockblock-report)]) :telemetry-report-type op)
                     (assoc (read-packet-data [op packet]) :telemetry-report-type op))
                   (catch Exception e (error-data rockblock-report (.getMessage e)))))]
    (merge meta result)))