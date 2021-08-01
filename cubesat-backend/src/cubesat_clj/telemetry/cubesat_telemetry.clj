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
   22  ::normal-report-faults
   34  ::special-report
   42  ::ttl
   ;69 ::special-report                                      ;deprecated? Use the 7x opcodes for specific reports
   ;70 ::imu
   71  ::imu
   72  ::temperature
   73  ::inhibs
   74  ::rbf
   75  ::current-sensor
   76  ::battery-voltage
   77  ::sd-card
   78  ::button
   255 ::ack})

(defn save-cubesat-data
  "Saves a cubesat report to elasticsearch"
  [data]
  (let [index (cfg/cubesat-db-index (cfg/get-config))]
    (es/index! index es/daily-index-strategy data)))

(defn save-ttl-data
  "Process image fragment data sent by cubesat. Image comes over several fragments as
  rockblock only supports so much protocol. Image 'fragments' are then assembled into full images
  when fully collected, and saved into the image database"
  [ttl-data]
  (let [{:keys [image-serial-number image-fragment-number image-data total]} ttl-data]
    (save-cubesat-data ttl-data)
    (img/save-fragment image-serial-number image-fragment-number image-data)
    (img/try-save-image image-serial-number total)))

(defn- map-range
  "Recreation of Arduino map() function used in flight code in order to convert imu data to correct imu values.
  https://www.arduino.cc/reference/en/language/functions/math/map/"
  [x in-min in-max out-min out-max]
  (+ out-min
     (* (/ (- out-max out-min)
           (- in-max in-min))
        (- x in-min))))

(defn- compute-imu-values
  "Computes correct IMU readings (unknown units for now) from IMU data read from a packet. Returns map with the correct
  values for IMU data, with keys named by: :<key>-value"
  [{:keys [msh-mag msh-gyro msh-acc
           x-mag y-mag z-mag
           x-gyro y-gyro z-gyro
           x-accel y-accel z-accel] :as cubesat-data}]
  (assoc cubesat-data
         :x-mag-value (map-range (float x-mag) 0 255 (- msh-mag) msh-mag)
         :y-mag-value (map-range (float y-mag) 0 255 (- msh-mag) msh-mag)
         :z-mag-value (map-range (float z-mag) 0 255 (- msh-mag) msh-mag)
         :x-gyro-value (map-range (float x-gyro) 0 255 (- msh-gyro) msh-gyro)
         :y-gyro-value (map-range (float y-gyro) 0 255 (- msh-gyro) msh-gyro)
         :z-gyro-value (map-range (float z-gyro) 0 255 (- msh-gyro) msh-gyro)
         :x-accel-value (map-range (float x-accel) 0 255 (- msh-acc) msh-acc)
         :y-accel-value (map-range (float y-accel) 0 255 (- msh-acc) msh-acc)
         :z-accel-value (map-range (float z-accel) 0 255 (- msh-acc) msh-acc)))

(defn- compute-temp-value
  "Computes correct temperature in degrees Celsius from mapped uint8 sensor reading in a packet.
  Uses a key named :temp-value for the result"
  [{:keys [temp] :as cubesat-data}]
  (let [real-temp (-> temp
                      float
                      (map-range 0 255 0 1023)
                      (* 3.3)
                      (/ 1024.0)
                      (- 0.5)
                      (* 100.0))]
    (assoc cubesat-data
           :temp-value real-temp)))

(defn- compute-current-value
  "Computes solar current value in amperes from mapped uint8 sensor reading in a packet.
  Uses a key named :solar-current-value for the result"
  [{:keys [solar-current] :as cubesat-data}]
  (let [msh-voltage-ref 3.3 ; What is this? TODO have the cubesat include it in packet. Placeholder magic number for now
        msh-rs 0.1 ; What is this? TODO have the cubesat include it in packet. Placeholder magic number for now
        msh-rl 29.75 ; What is this? TODO have the cubesat include it in packet. Placeholder magic number for now
        real-current (-> solar-current
                         float
                         (map-range 0 255 0 1023)
                         (* msh-voltage-ref)
                         (/ 1023.0)
                         (/ (* msh-rl msh-rs)))]
    (assoc cubesat-data
           :solar-current-value real-current)))

(defn- compute-battery-value
  "Computes battery voltage in volts from mapped uint8 sensor reading in a packet.
  Uses a key named :battery-voltage-value for the result"
  [{:keys [battery] :as cubesat-data}]
  (let [msh-voltage-ref 3.3
        r1 470
        r2 1000
        real-voltage (-> battery
                         float
                         (map-range 0 255 0 1023)
                         (* msh-voltage-ref)
                         (/ 1024.0)
                         (* (+ r1 r2))
                         (/ r2))]
    (assoc cubesat-data
           :battery-voltage-value real-voltage)))

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
   fragment is the last fragment, which is indicated by the end-marker 'FFD9'. 
   Returns the max number of fragments for the image, the hex string data needed
   to be read, and the number of bytes of the fragment contents data (not 
   counting image serial number and fragment number).
   
   If the image fragment is not last, then the max number of fragments is set 
   arbitrarilty to 100, and the entirety of the hex string must be read. If the 
   image fragment is the last, then the max number of fragments is set to 
   (last fragment number + 1), and the hexadecimal string is read up to 'FFD9.'
   
   Notes: - The fragment number is stored in the indices [10, 12) of the hex 
            string.
          - The first byte in the hex string is the op code, and has already 
            been read,
            so it is omitted.
          - The entire hex string for an image fragment data is 70 bytes long 
            (140 characters)."
  [rockblock-data]
  (if (str/includes? rockblock-data "FFD9")
    (let [max-fragments (+ 1 (Integer/parseInt (subs rockblock-data 10 12) 16))
          end-boundary (+ 4 (str/index-of rockblock-data "FFD9"))
          hex-data (subs rockblock-data 2 end-boundary)
          byte-length (/ (- (.length hex-data) 10) 2)]
      {:max-fragments max-fragments, :hex-data hex-data, :byte-length byte-length})
    (let [max-fragments 100
          hex-data (subs rockblock-data 2 140)
          byte-length 64]
      {:max-fragments max-fragments, :hex-data hex-data, :byte-length byte-length})))

(defmulti read-packet-data
  "Reads data from a packet based on opcode.
          Note: Image data is received in fragments, :data-length bytes each, which must be assembled into a full image
          after receiving all fragments. The serial number is which image is being sent, and the fragment number
          is which part of the image being sent"
  (fn [[opcode packet]] opcode))

(defmethod read-packet-data ::ttl
  [[_ rockblock-data]]
  (let [fragment-info (read-hex-fragment rockblock-data)
        packet (rb/get-cubesat-message-binary (:hex-data fragment-info))
        metadata (reader/read-structure
                  packet
                  [:image-serial-number ::reader/uint8
                   :image-fragment-number ::reader/uint32])
        fragment (reader/read-structure
                  packet
                  [:image-data ::reader/byte-array (:byte-length fragment-info)])
        total (:total (:max-fragments fragment-info))]
    (merge metadata fragment total)))

(defmethod read-packet-data ::imu
  [[_ packet]]
  (-> packet
      (reader/read-structure
       [:msh-mag ::reader/uint8
        :msh-gyro ::reader/uint8
        :msh-acc ::reader/uint8
        :x-mag ::reader/uint8
        :y-mag ::reader/uint8
        :z-mag ::reader/uint8
        :x-gyro ::reader/uint8
        :y-gyro ::reader/uint8
        :z-gyro ::reader/uint8
        :x-accel ::reader/uint8
        :y-accel ::reader/uint8
        :z-accel ::reader/uint8
        :imu-temp ::reader/uint8
        :placeholder ::reader/uint8])
      compute-imu-values))


;; (defmethod read-packet-data ::normal-report
;;   [[_ packet]]
;;   (-> packet
;;       (reader/read-structure
;;         [:msh-mag ::reader/uint8
;;          :msh-gyro ::reader/uint8
;;          :msh-acc ::reader/uint8
;;          :x-mag ::reader/uint8
;;          :y-mag ::reader/uint8
;;          :z-mag ::reader/uint8
;;          :x-gyro ::reader/uint8
;;          :y-gyro ::reader/uint8
;;          :z-gyro ::reader/uint8
;;          :x-accel ::reader/uint8
;;          :y-accel ::reader/uint8
;;          :z-accel ::reader/uint8
;;          :imu-temp ::reader/uint8
;;          :temp ::reader/uint8
;;          :solar-current ::reader/uint8
;;          :battery ::reader/uint8])
;;       compute-imu-values
;;       compute-temp-value
;;       compute-current-value
;;       compute-battery-value))


(defmethod read-packet-data ::normal-report
  [[_ packet]]
  (-> packet
      (reader/read-structure
       [:photoresistor-covered ::reader/uint8
        :door-button-pressed ::reader/uint8
        :mission-mode ::reader/uint8
        :fire-burnwire ::reader/uint8
        :arm-burnwire ::reader/uint8
        :burnwire-burn-time ::reader/uint8
        :burnwire-armed-timeout ::reader/uint8
        :burnwire-mode ::reader/uint8
        :burnwire-attempts ::reader/uint8
        :downlink-period ::reader/uint8
        :waiting-messages ::reader/uint8
        :command-to-process ::reader/uint8
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
        :battery ::reader/uint8
        :fault-mode ::reader/uint8
        :check-mag-x ::reader/uint8
        :check-mag-y ::reader/uint8
        :check-mag-z ::reader/uint8
        :check-gyro-x ::reader/uint8
        :check-gyro-y ::reader/uint8
        :check-gyro-z ::reader/uint8
        :check-temp ::reader/uint8
        :check-solar ::reader/uint8
        :check-battery ::reader/uint8
        :take-photo ::reader/uint8
        :camera-on ::reader/uint8])))

(defmethod read-packet-data ::special-report
  [[_ packet]]
  (-> packet
      (reader/read-structure
       [:msh-imu-active ::reader/uint8
        :msh-mag ::reader/uint8
        :msh-gyro ::reader/uint8
        :msh-acc ::reader/uint8
        :x-mag ::reader/uint8
        :y-mag ::reader/uint8
        :z-mag ::reader/uint8
        :x-gyro ::reader/uint8
        :y-gyro ::reader/uint8
        :z-gyro ::reader/uint8
        :x-accel ::reader/uint8
        :y-accel ::reader/uint8
        :z-accel ::reader/uint8
        :msh-mag-log-0 ::reader/uint8
        :msh-gyro-log-0 ::reader/uint8
        :msh-accel-log-0 ::reader/uint8
        :msh-mag-log-1 ::reader/uint8
        :msh-gyro-log-1 ::reader/uint8
        :msh-accel-log-1 ::reader/uint8
        :msh-mag-log-2 ::reader/uint8
        :msh-gyro-log-2 ::reader/uint8
        :msh-accel-log-2 ::reader/uint8
        :imu-temp ::reader/uint8
        :temp ::reader/uint8
        :solar-current ::reader/uint8
        :battery ::reader/uint8
        :door-button ::reader/uint8
        :inhibitor-19a ::reader/uint8
        :inhibitor-10b ::reader/uint8
        :inhibitor-2 ::reader/uint8
        :free-hub ::reader/uint8
        :next-mode ::reader/uint8
        :downlink-size ::reader/uint8
        :downlink-period ::reader/uint8
        :uplink-period ::reader/uint8
        :mt-queued ::reader/uint8
        :sbdix-fails ::reader/uint8
        :low-power-timer ::reader/uint8])
      compute-imu-values
      compute-temp-value
      compute-current-value
      compute-battery-value))

(defmethod read-packet-data ::ack
  [[_ packet]]
  (reader/read-structure
   packet
   [:ack-first-byte ::reader/uint8
    :ack-second-byte ::reader/uint8]))

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