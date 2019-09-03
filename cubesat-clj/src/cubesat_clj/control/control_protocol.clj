(ns cubesat-clj.control.control-protocol
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


(s/defschema Command
  "Supported uplink commands. Original names from Alpha docs in comments"
  {:imei      s/Str
   :operation (s/conditional
                ;; TODO the rest of them

                (is-type? :report) {:type (s/eq :report)}

                (is-type? :imu) {:type (s/eq :imu)}

                (is-type? :echo) {:type  (s/eq :echo)
                                  :input s/Str})})


(s/defschema Macro
  "Multiple commands grouped together"
  [Command])


;; ---------------------- Uplink Commands -------------------------

(def ^:const rockblock-endpoint
  "Endpoint for submitting commands to rockblock"
  "https://core.rock7.com/rockblock/MT")


;;TODO the rest of them
(def ^:const uplink-opcodes
  "Uplibnk command opcodes by command type"
  {:report 1
   :imu 4
   :echo 5})


(defn- get-no-arg
  "Since most commands are 0 or 1 argument; a helper function
  that returns the string representation for a no-argument
  operation

  Example:
    (get-no-arg 20)
    returns the string '20,' "
  [operation]
  (str (-> operation :type uplink-opcodes) ","))


(defn- parse-single-arg
  "Since most commands are 0 or 1 argument; a helper function
  that returns the string representation for a single argument
  operation

  Example:
    (parse-single-arg {:type :report :example 50} :example)
    returns the string '1,50' "
  [operation key]
  (str (-> operation :type uplink-opcodes) "," (key operation)))


(defn parse-command-args
  "Takes a Command and translates it to a string representation as
  specified in the Alpha documentation"
  [{operation :operation}]
  (case (:type operation)
    ;TODO the rest of them
    :report (get-no-arg operation)
    :imu (get-no-arg operation)
    :echo (parse-single-arg operation :input)))


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
