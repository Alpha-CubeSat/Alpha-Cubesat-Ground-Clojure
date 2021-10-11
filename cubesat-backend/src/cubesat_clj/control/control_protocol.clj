(ns cubesat-clj.control.control-protocol
  "Format of incoming and outgoing data concerning control, and the message
  format used by the cubesat for commands."
  (:require [clj-http.client :as http]
            [cubesat-clj.util.binary.hex-string :as bin]
            [schema.core :as s]
            [cuerdas.core :as str]
            [clojure.string :as string]))


;; ---------------------- Ground API ------------------------------


(defn- is-type?
  "Returns a function that determines if a command
  operation is of the given type"
  [type]
  (fn [operation]
    (= (:type operation) type)))


;; TODO Support JSON. Having keyword values does not play nice with JSON, need to use strings in spec
;; and convert to keywords later


(s/defschema Command
  "Supported uplink commands. Original names from Alpha docs in comments"
  {:type s/Keyword
   :fields {s/Any s/Any}})

(s/defschema CommandResponse
  {:status s/Str
   (s/optional-key :id) s/Str
   (s/optional-key :error-code) s/Str
   (s/optional-key :desc) s/Str})

(s/defschema Macro
  "Multiple commands grouped together"
  [Command])


;; ---------------------- Uplink Commands -------------------------


(def ^:const rockblock-endpoint
  "Endpoint for submitting commands to rockblock"
  "https://core.rock7.com/rockblock/MT")

(def ^:const uplink-opcodes
  "Uplink command opcodes by command type"
  {:burnwire-burn-time "0300"
   :burnwire-arm-time "0400"
   :rockblock-downlink-period "0500"
   :request-img-fragment "0600"})

(defn- flip-bytes
  "Flips a hexadecimal sequence of 4 bytes so that it is ready to submitted as 
   a command argument.
   
   Example:
   
   '00000154' (or 00 00 01 54) -> '54010000' (or 54 01 00 00)"
  [hex-string]
  (let [s1 (subs hex-string 0 2)
        s2 (subs hex-string 2 4)
        s3 (subs hex-string 4 6)
        s4 (subs hex-string 6 8)]
    (str s4 s3 s2 s1)))

(defn- pad-hex-string
  "Pads a hex string with with '0's at the beginning until it is length 8."
  [s]
  (if (< (.length s) 8)
    (recur (str "0" s))
    (flip-bytes s)))

(defn- hexify-arg
  "Translates decimal number into a hexidecimal string."
  [s]
  (let [hex (string/upper-case (format "%x" (Integer/parseInt s)))]
    (pad-hex-string hex)))

(defn- parse-single-arg
  "A helper function that returns the string representation for a single 
   argument operation

  Example:
    (parse-single-arg {:type :burnwire-burn-time :example 3} :example)
    returns the string '03000300000000000000' ('0300' + '03000000' + '00000000')"
  [operation key]
  (str (-> operation :type uplink-opcodes) (-> operation :fields key hexify-arg) "00000000"))

(defn- parse-double-arg
  [operation key1 key2]
  (str (-> operation :type uplink-opcodes) (-> operation :fields key1 hexify-arg) (-> operation :fields key2 hexify-arg)))

(defn parse-command-args
  "Takes a Command and translates it to a string representation as
  specified in the Alpha documentation"
  [operation]
  (case (:type operation)
    :mission-mode-init         "00000000000000000000"
    :mission-mode-low-power    "00000100000000000000"
    :mission-mode-deployment   "00000200000000000000"
    :mission-mode-standby      "00000300000000000000"
    :mission-mode-safe         "00000400000000000000"
    :burnwire-arm-true         "01000100000000000000"
    :burnwire-arm-false        "01000000000000000000"
    :burnwire-fire-true        "02000100000000000000"
    :burnwire-fire-false       "02000000000000000000"
    :burnwire-burn-time        (parse-single-arg operation :burn-time)
    :burnwire-arm-time         (parse-single-arg operation :arm-time)
    :rockblock-downlink-period (parse-single-arg operation :downlink-period)
    :request-img-fragment      (parse-double-arg operation :camera-number :img-fragment)
    :take-photo-true           "07000100000000000000"
    :take-photo-false          "07000000000000000000"
    :temperature-mode-active   "08000100000000000000"
    :temperature-mode-inactive "08000000000000000000"
    :acs-mode-full             "09000200000000000000"
    :acs-mode-simple           "09000100000000000000"
    :acs-mode-off              "09000000000000000000"
    :acs-mag-x                 "0A000000000000000000"
    :acs-mag-y                 "0A000100000000000000"
    :acs-mag-z                 "0A000200000000000000"
    :camera-turn-on            "0B000100000000000000"
    :camera-turn-off           "0C000100000000000000"
    :fault-mode-active         "F1FF0100000000000000"
    :fault-mode-inactive       "F1FF0000000000000000"
    :fault-check-mag-x-true    "F2FF0100000000000000"
    :fault-check-mag-x-false   "F2FF0000000000000000"
    :fault-check-mag-y-true    "F3FF0100000000000000"
    :fault-check-mag-y-false   "F3FF0000000000000000"
    :fault-check-mag-z-true    "F4FF0100000000000000"
    :fault-check-mag-z-false   "F4FF0000000000000000"
    :fault-check-gyro-x-true   "F5FF0100000000000000"
    :fault-check-gyro-x-false  "F5FF0000000000000000"
    :fault-check-gyro-y-true   "F6FF0100000000000000"
    :fault-check-gyro-y-false  "F6FF0000000000000000"
    :fault-check-gyro-z-true   "F7FF0100000000000000"
    :fault-check-gyro-z-false  "F7FF0000000000000000"
    :fault-check-temp-c-true   "F8FF0100000000000000"
    :fault-check-temp-c-false  "F8FF0000000000000000"
    :fault-check-solar-true    "F9FF0100000000000000"
    :fault-check-solar-false   "F9FF0000000000000000"
    :fault-check-voltage-true  "FAFF0100000000000000"
    :fault-check-voltage-false "FAFF0000000000000000"))

(defn send-uplink
  "Sends an uplink request to the satellite via the
  Rockblock API. Requires rockblock user, password, and
  imei for the radio, as well as the string data to send. Returns
  the rockblock web services response data as a map with keys [:status :id :error-code :description].
  :description and :code are only returned when there is an error, and :id is only returned on success
  The possible responses are documented at https://www.rock7.com/downloads/RockBLOCK-Web-Services-User-Guide.pdf"
  [imei user pass data]
  (let [request {:imei     imei
                 :username user
                 :password pass
                 :data     data}
        response (http/post rockblock-endpoint {:form-params request})
        [status code desc] (str/split (:body response) ",")
        response-map (conj {:status status}
                           (when (= status "FAILED") {:desc desc :error-code code})
                           (when (= status "SUCCESS") {:id code}))]
    (println "GOT RESPONSE: " response-map)
    response-map))