(ns cubesat-clj.control.control-protocol
  "Format of incoming and outgoing data concerning control, and the message
  format used by the cubesat for commands."
  (:require [clj-http.client :as http]
            [cubesat-clj.util.binary.hex-string :as bin]
            [schema.core :as s]))


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
  {:report 1
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
  (str (-> operation :type uplink-opcodes pad-single-digit) "," (-> operation key pad-single-digit) "!"))


(defn parse-command-args
  "Takes a Command and translates it to a string representation as
  specified in the Alpha documentation"
  [operation]
  (case (:type operation)
    ;TODO the rest of them
    :report (get-no-arg operation)
    :imu (get-no-arg operation)
    :echo (parse-single-arg operation :input)))


;TODO parse the RB response code according to their documentation
(defn send-uplink
  "Sends an uplink request to the satellite via the
  Rockblock API. Requires rockblock user, password, and
  imei for the radio, as well as the string data to send"
  [imei user pass data]
  (let [binary (bin/hexify data)
        request {:imei     imei
                 :username user
                 :password pass
                 :data     binary}
        response (http/post rockblock-endpoint {:form-params request})]
    (println "GOT RESPONSE: " response)))
