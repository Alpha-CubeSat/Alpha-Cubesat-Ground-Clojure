(ns cubesat-clj.telemetry.rockblock-telemetry
  (:require [buddy.core.keys :as keys]
            [cubesat-clj.util.binary.hex-string :as hex]
            [cubesat-clj.util.binary.byte-buffer :as buffer]
            [buddy.sign.jwt :as jwt]
            [schema.core :as s]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.databases.elasticsearch :as es]))

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
  (try
    (let [jwt (:JWT rockblock-report)
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


(defn save-rockblock-report
  "Saves a rockblock report to elasticsearch. Gets latitude and longitude
   data and makes a single field out of them"
  [data]
  (let [lat (:iridium_latitude data)
        lon (:iridium_longitude data)
        location {:lat lat, :lon lon}
        result (assoc data :location location)
        index (cfg/rockblock-db-index (cfg/get-config))]
    (es/index! index es/daily-index-strategy result)))


(defn fix-rb-datetime
  "Convert Rockblock's nonstandard date format to YYYY-MM-DDThh:mm:ssZ.
  Rockblock uses YY-MM-DD HH:mm:ss as the date format, despite their documentation claiming to use a more standard
  format: YYYY-MM-DDThh:mm:ssZ. Conversion is done by appending '20' to the start of the date string,
  which means this fix may not work after the year 2100."
  [dt]
  (str "20" (.replace dt " " "T") "Z"))