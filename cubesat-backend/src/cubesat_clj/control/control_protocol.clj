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
  {:rockblock-downlink-period "0100"
   :request-img-fragment "0200"})

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
    :mission-mode-standby         "00000000000000000000"
    :mission-mode-high-altitude   "00000100000000000000"
    :mission-mode-deployment      "00000200000000000000"
    :mission-mode-post-deployment "00000300000000000000"
    :rockblock-downlink-period    (parse-single-arg operation :downlink-period)
    :request-img-fragment         (parse-double-arg operation :camera-number :img-fragment)))

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