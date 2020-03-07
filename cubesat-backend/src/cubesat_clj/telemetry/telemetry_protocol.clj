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
  {(s/optional-key :id)                s/Str
   (s/optional-key :transport)         s/Str
   :imei                               s/Str
   :device_type                        s/Str
   :serial                             s/Int
   :momsn                              s/Int
   :transmit_time                      s/Inst
   :data                               s/Str
   (s/optional-key :message)           s/Str
   (s/optional-key :at)                s/Inst
   :JWT                                s/Str
   :iridium_longitude                  s/Num
   :iridium_latitude                   s/Num
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
   s/Any                               s/Any})


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


(defn get-cubesat-message-binary
  "Gets the string encoded binary data sent by the cubesat as a java nio ByteBuffer"
  [rockblock-report]
  (-> (:data rockblock-report)
      (hex/hex-str-to-bytes)
      (buffer/from-byte-array)))


;;------------------------------- CUBESAT DATA -------------------------------------------------------------------------

(def ^:const opcodes
  "Packet opcodes for cubesat (see Alpha documentation for specification)"
  {21 ::normal-report
   22 ::normal-report-faults
   34 ::special-report
   42 ::ttl
   ;69 ::special-report                                      ;deprecated? Use the 7x opcodes for specific reports
   ;70 ::imu
   71 ::imu
   72 ::temperature
   73 ::inhibs
   74 ::rbf
   75 ::current-sensor
   76 ::battery-voltage
   77 ::sd-card
   78 ::button
   255 ::ack})


(defn read-opcode
  "Reads the opcode of an incoming packet"
  [packet]
  (if (= (reader/remaining packet) 0)
    ::empty-packet
    (-> packet
        (reader/read-uint8)
        opcodes)))


(defn read-image-data
  "Reads image data from a packet using the specified cubesat protocol in the Alpha documentation.
  Image is received in fragments, :data-length bytes each, which must be assembled into the full image
  after receiving all fragments. The serial number is which image is being sent, and the fragment number
  is which part of the image being sent"
  [packet]
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


(defn read-imu-data
  "Reads IMU data from an incoming packet using cubesat protocol specified in the Alpha documentation"
  [packet]
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


(defn read-normal-report
  "Reads a normal report from an incoming packet"
  [packet]
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


(defn read-special-report
  "Reads a special report from an incoming packet"
  [packet]
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


(defn read-ack
  "Reads acknowledgement/success data from the packet"
  [packet]
  (reader/read-structure
    packet
    [:ack-first-byte ::reader/uint8
     :ack-second-byte ::reader/uint8]))