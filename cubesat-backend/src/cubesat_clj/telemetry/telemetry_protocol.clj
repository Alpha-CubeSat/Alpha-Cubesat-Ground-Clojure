(ns cubesat-clj.telemetry.telemetry-protocol
  "Specifies/parses formats used by the RockBlock API and cubesat.
  These formats are described in the RockBlock web services docs at:
  https://docs.rock7.com/reference#push-api
  And for the satellite in the Google Drive for Alpha"
  (:require [schema.core :as s]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [buddy.core.keys :as keys]
            [cheshire.core :as json]
            [cubesat-clj.util.binary.byte-buffer :as buffer]
            [cubesat-clj.util.binary.binary-reader :as reader]
            [cubesat-clj.util.binary.hex-string :as hex]))


;;---------------------------- ROCKBLOCK DATA --------------------------------------------------------------------------

(s/defschema RockblockReport
  "A report submitted by the rockblock API. Contains main fields with satellite
  data and information, along with optionally sent fields, and some unknown ones
  that may not be consistent with the rockblock docs.

  The 'data' field contains the hex-encoded binary data sent by the satellite."
  ; TODO fix how the jwt data is all strings, which breaks verification
  {;(s/optional-key :id)                s/Str
   ;(s/optional-key :transport)         s/Str
   ;:imei                               s/Str
   ;:device_type                        s/Str
   ;:serial                             s/Int
   ;:momsn                              s/Int
   ;:transmit_time                      s/Inst
   ;:data                               s/Str
   ;(s/optional-key :message)           s/Str
   ;(s/optional-key :at)                s/Inst
   ;:JWT                                s/Str
   ;:iridium_longitude                  s/Num
   ;:iridium_latitude                   s/Num
   ;(s/optional-key :cep)               s/Int
   ;(s/optional-key :trigger)           s/Str
   ;(s/optional-key :source)            s/Str
   ;(s/optional-key :lat)               s/Num
   ;(s/optional-key :lon)               s/Num
   ;(s/optional-key :sog)               s/Num
   ;(s/optional-key :cog)               s/Num
   ;(s/optional-key :alt)               s/Num
   ;(s/optional-key :temp)              s/Num
   ;(s/optional-key :battery)           s/Num
   ;(s/optional-key :power)             s/Bool
   ;(s/optional-key :ack_request)       s/Int
   ;(s/optional-key :message_ack)       s/Int
   ;(s/optional-key :alert)             s/Bool
   ;(s/optional-key :waypoint)          s/Str
   ;(s/optional-key :appMessageAddress) [s/Str]
   ;(s/optional-key :appMessageContent) s/Str
   ;(s/optional-key :beacons)           [s/Str]
   s/Any s/Any})


(def rockblock-web-pk
  "Public key provided for JWT verification by rockblock web services documentation"
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlaWAVJfNWC4XfnRx96p9cztBcdQV6l8aKmzAlZdpEcQR6MSPzlgvihaUHNJgKm8t5ShR3jcDXIOI7er30cIN4/9aVFMe0LWZClUGgCSLc3rrMD4FzgOJ4ibD8scVyER/sirRzf5/dswJedEiMte1ElMQy2M6IWBACry9u12kIqG0HrhaQOzc6Tr8pHUWTKft3xwGpxCkV+K1N+9HCKFccbwb8okRP6FFAMm5sBbw4yAu39IVvcSL43Tucaa79FzOmfGs5mMvQfvO1ua7cOLKfAwkhxEjirC0/RYX7Wio5yL6jmykAHJqFG2HT0uyjjrQWMtoGgwv9cIcI7xbsDX6owIDAQAB\n-----END PUBLIC KEY-----"))


(defn verify-rockblock-request
  "Uses jwt to verify data sent by rockblock web services. Returns a copy of the data if valid,
  nil if invalid/corrupt"
  [rockblock-report]
  (try (let [jwt (:JWT rockblock-report)
             unsigned-data (jwt/unsign jwt rockblock-web-pk {:alg :rs256})
             edn-data (clojure.walk/keywordize-keys unsigned-data)] ;Have to convert the decoded json to edn, even though original, unencoded, request was in edn
         (assoc edn-data :JWT jwt))
       (catch Exception e
         (do (str "Caught exception unsigning rockblock data: " (.printStackTrace e))
             nil))))


(defn- get-cubesat-message-binary
  "Gets the string encoded binary data sent by the cubesat as a java nio ByteBuffer"
  [rockblock-report]
  (-> (:data rockblock-report)
      (hex/hex-str-to-bytes)
      (buffer/from-byte-array)))


;;------------------------------- CUBESAT DATA -------------------------------------------------------------------------

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


(defn- read-opcode
  "Reads the opcode of an incoming packet. If empty packet is received, returns ::empry-packet instead"
  [packet]
  (if (= (reader/remaining packet) 0)
    ::empty-packet
    (-> packet
        (reader/read-uint8)
        opcodes)))


(defmulti read-packet-data
          "Reads data from a packet based on opcode.
          Note: Image data is received in fragments, :data-length bytes each, which must be assembled into a full image
          after receiving all fragments. The serial number is which image is being sent, and the fragment number
          is which part of the image being sent"
          (fn [[opcode packet]] opcode))

(defmethod read-packet-data ::ttl
  [[_ packet]]
  (let [metadata (reader/read-structure
                   packet
                   [:image-serial-number ::reader/uint16
                    :image-fragment-number ::reader/uint8
                    :image-max-fragments ::reader/uint8
                    :data-length ::reader/uint8])
        fragment (reader/read-structure
                   packet
                   [:image-data ::reader/byte-array (:data-length metadata)])]
    (merge metadata fragment)))


(defmethod read-packet-data ::imu
  [[_ packet]]
  (reader/read-structure
    packet
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
     :placeholder ::reader/uint8]))


(defmethod read-packet-data ::normal-report
  [[_ packet]]
  (reader/read-structure
    packet
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
     :temp ::reader/uint8
     :solar-current ::reader/uint8
     :battery ::reader/uint8
     :placeholder ::reader/uint8]))


(defmethod read-packet-data ::special-report
  [[_ packet]]
  (reader/read-structure
    packet
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
     :low-power-timer ::reader/uint8
     :placeholder ::reader/uint8]))


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
  (let [packet (get-cubesat-message-binary rockblock-report)
        op (read-opcode packet)
        meta (report-metadata rockblock-report)
        result (if (= op ::empty-packet)
                 (error-data rockblock-report "empty packet")
                 (try
                   (assoc (read-packet-data [op packet]) :telemetry-report-type op)
                   (catch Exception e (error-data rockblock-report (.getMessage e)))))]
    (merge meta result)))