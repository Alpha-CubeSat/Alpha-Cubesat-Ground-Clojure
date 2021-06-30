(ns cubesat-clj.control.control-protocol
  "Format of incoming and outgoing data concerning control, and the message
  format used by the cubesat for commands."
  (:require [clj-http.client :as http]
            [cubesat-clj.util.binary.hex-string :as bin]
            [schema.core :as s]
            [cuerdas.core :as str]))


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


;;TODO the rest of them


(def ^:const uplink-opcodes
  "Uplink command opcodes by command type"
  {:report 3
   :imu    4
   :echo   5})

(defn- pad-single-digit
  "Pads a string with '0's (the character '0', not the number) at the beginning until it is length 2"
  [st]
  (let [s (str st)]
    (if (< (.length s) 2)
      (recur (str "0" s))
      s)))

(defn- get-no-arg
  "Since most commands are 0 or 1 argument; a helper function
  that returns the string representation for a no-argument
  operation

  Example:
    (get-no-arg 20)
    returns the string '20,00!'"
  [operation]
  (str (-> operation :type uplink-opcodes pad-single-digit) ",00" "!"))

(defn- parse-single-arg
  "Since most commands are 0 or 1 argument; a helper function
  that returns the string representation for a single argument
  operation

  Example:
    (parse-single-arg {:type :report :example 50} :example)
    returns the string '01,50!'"
  [operation key]
  (str (-> operation :type uplink-opcodes pad-single-digit) "," (-> operation :fields key pad-single-digit) "!"))

(defn parse-command-args
  "Takes a Command and translates it to a string representation as
  specified in the Alpha documentation"
  [operation]
  (case (:type operation)
    ;TODO the rest of them
    :mission-mode-low-power    "00000100000000000000"
    :mission-mode-deployment   "00000000000000000000"
    :mission-mode-standby      "00000200000000000000"
    :mission-mode-safe         "00000300000000000000"
    :burnwire-arm-true         "01000100000000000000"
    :burnwire-arm-false        "01000000000000000000"
    :burnwire-fire-true        "02000100000000000000"
    :burnwire-fire-false       "02000000000000000000"
    :rockblock-downlink-period "04000000000000000000"
    :request-img-fragment      "05000000000000000000"
    :take-photo-true           "06000100000000000000"
    :take-photo-false          "06000000000000000000"
    :temperature-mode-active   "07000100000000000000"
    :temperature-mode-inactive "07000000000000000000"
    :acs-mode-detumbling       "08000200000000000000"
    :acs-mode-pointing         "08000100000000000000"
    :acs-mode-off              "08000000000000000000"
    :fault-mode-active         "09000100000000000000"
    :fault-mode-inactive       "09000000000000000000"
    :fault-check-mag-x-true    "F4FF0100000000000000"
    :fault-check-mag-x-false   "F4FF0000000000000000"
    :fault-check-mag-y-true    "F5FF0100000000000000"
    :fault-check-mag-y-false   "F5FF0000000000000000"
    :fault-check-mag-z-true    "F6FF0100000000000000"
    :fault-check-mag-z-false   "F5FF0000000000000000"
    :fault-check-gyro-x-true   "F7FF0100000000000000"
    :fault-check-gyro-x-false  "F7FF0000000000000000"
    :fault-check-gyro-y-true   "F8FF0100000000000000"
    :fault-check-gyro-y-false  "F8FF0000000000000000"
    :fault-check-gyro-z-true   "F9FF0100000000000000"
    :fault-check-gyro-z-false  "F9FF0000000000000000"
    :fault-check-temp-c-true   "FDFF0100000000000000"
    :fault-check-temp-c-false  "FDFF0000000000000000"
    :fault-check-solar-true    "FEFF0100000000000000"
    :fault-check-solar-false   "FEFF0000000000000000"
    :fault-check-voltage-true  "FFFF0100000000000000"
    :fault-check-voltage-false "FFFF0000000000000000"
    ))

(defn send-uplink
  "Sends an uplink request to the satellite via the
  Rockblock API. Requires rockblock user, password, and
  imei for the radio, as well as the string data to send. Returns
  the rockblock web services response data as a map with keys [:status :id :error-code :description].
  :description and :code are only returned when there is an error, and :id is only returned on success
  The possible responses are documented at https://www.rock7.com/downloads/RockBLOCK-Web-Services-User-Guide.pdf"
  [imei user pass data]
  (let [binary (bin/hexify data)
        request {:imei     imei
                 :username user
                 :password pass
                 :data     binary}
        response (http/post rockblock-endpoint {:form-params request})
        [status code desc] (str/split (:body response) ",")
        response-map (conj {:status status}
                           (when (= status "FAILED") {:desc desc :error-code code})
                           (when (= status "SUCCESS") {:id code}))]
    (println "GOT RESPONSE: " response-map)
    response-map))