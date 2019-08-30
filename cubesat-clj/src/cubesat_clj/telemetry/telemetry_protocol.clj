(ns cubesat-clj.telemetry.telemetry-protocol
  "Specifies/parses formats used by the RockBlock API and cubesat"
  (:require [schema.core :as s]))

(s/defschema RockblockReport
  "A report submitted by the rockblock API. Contains main fields with satellite
  data and information, along with optionally sent fields, and some unknown ones
  that may not be consistent with the rockblock docs.

  The 'message' field contains the hex-encoded binary data sent by the satellite."
  {:id                                 s/Str
   :transport                          s/Str
   :imei                               s/Str
   :device_type                        s/Str
   :serial                             s/Int
   :momsn                              s/Int
   :transmit_time                      s/Inst
   :message                            s/Str
   :at                                 s/Inst
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