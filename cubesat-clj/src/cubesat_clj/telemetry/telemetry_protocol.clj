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

  The 'message' field contains the hex-encoded binary data sent by the satellite."
  {:id                                 s/Str
   :transport                          s/Str
   ;:imei                               s/Str
   :device_type                        s/Str
   :serial                             s/Int
   :momsn                              s/Int
   :transmit_time                      s/Inst
   ;:message                            s/Str
   :at                                 s/Inst
   :JWT                                s/Str
   (s/optional-key :iridium_longitude) s/Num
   (s/optional-key :iridium_latitude)  s/Num
   (s/optional-key :cep)               s/Int
   (s/optional-key :trigger)           s/Str
   (s/optional-key :source)            s/Str
   (s/optional-key :lat)               s/Num
   (s/optional-key :lon)               s/Num
   (s/optional-key :sog)               s/Num
   (s/optional-key :cog)               s/Num
   (s/optional-key :alt)               s/Num
   (s/optional-key :temp)              s/Num
   (s/optional-key :battery)           s/Num
   (s/optional-key :power)             s/Bool
   (s/optional-key :ack_request)       s/Int
   (s/optional-key :message_ack)       s/Int
   (s/optional-key :alert)             s/Bool
   (s/optional-key :waypoint)          s/Str
   (s/optional-key :appMessageAddress) [s/Str]
   (s/optional-key :appMessageContent) s/Str
   (s/optional-key :beacons)           [s/Str]
   s/Any s/Any})

(def rockblock-web-pk
  "Public key provided for JWT verification by rockblock web services documentation"
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlaWAVJfNWC4XfnRx96p9cztBcdQV6l8aKmzAlZdpEcQR6MSPzlgvihaUHNJgKm8t5ShR3jcDXIOI7er30cIN4/9aVFMe0LWZClUGgCSLc3rrMD4FzgOJ4ibD8scVyER/sirRzf5/dswJedEiMte1ElMQy2M6IWBACry9u12kIqG0HrhaQOzc6Tr8pHUWTKft3xwGpxCkV+K1N+9HCKFccbwb8okRP6FFAMm5sBbw4yAu39IVvcSL43Tucaa79FzOmfGs5mMvQfvO1ua7cOLKfAwkhxEjirC0/RYX7Wio5yL6jmykAHJqFG2HT0uyjjrQWMtoGgwv9cIcI7xbsDX6owIDAQAB\n-----END PUBLIC KEY-----"))

(defn verify-rockblock-request [rockblock-report]
  "Uses jwt to verify data sent by rockblock web services. Returns the data if valid,
  nil if invalid/corrupt"
  (try (let [jwt (:JWT rockblock-report)
             unsigned-data (jwt/unsign jwt rockblock-web-pk {:alg :rs256})]
         (clojure.walk/keywordize-keys unsigned-data))      ;Have to convert the decoded json to edn, even though original, unencoded, request was in edn
       (catch Exception e
         (do (str "Caught exception unsigning rockblock data: " (.printStackTrace e))
             nil))))

(defn get-cubesat-message-binary
  "Gets the string encoded binary data sent by the cubesat as a java nio ByteBuffer"
  [rockblock-report]
  (-> (:message rockblock-report)
      (hex/hex-str-to-bytes)
      (buffer/from-byte-array)))


;;------------------------------- CUBESAT DATA -------------------------------------------------------------------------

(def ^:const opcodes
  "Packet opcodes for cubesat (see Alpha documentation for specification)"
  {21 ::normal-report
   22 ::normal-report-faults
   42 ::ttl
   69 ::special-report ;deprecated? Use the 7x opcodes for specific reports
   70 ::imu
   71 ::photoresistor
   72 ::temperature
   73 ::inhibs
   74 ::rbf
   75 ::current-sensor
   76 ::battery-voltage
   77 ::sd-card
   78 ::button})

(defn read-opcode
  "Reads the opcode of an incoming packet"
  [packet]
  (-> packet
      (reader/read-uint8)
      opcodes))

(defn read-image-data
  "Reads image data from a packet using the specified cubesat protocol in the Alpha documentation.
  Image is received in fragments, 66 bytes each, which must be assembled into the full image
  after receiving all fragments. The serial number is which image is being sent, and the fragment number
  is which part of the image being sent"
  [packet]
  (reader/read-structure packet
                         [:image-serial-number ::reader/uint16
                          :image-fragment-number ::reader/uint8
                          :raw-image-data ::reader/byte-array 66]))

(defn read-imu-data
  "Reads IMU data from an incoming packet using cubesat protocol specified in the Alpha documentation"
  [packet]
  (reader/read-structure packet
                         [:x-mag ::reader/uint8
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
                          :battery-voltage ::reader/uint8]))