(ns control-frontend.commands
  (:require [clojure.string :as str])
  (:require-macros [control-frontend.command-macros :refer [defcommand defcategory]]))

(defcommand change-sr
  "Change SR"
  "Changes the SR frequency to the desired frequency."
  :change-sr
  [{:title "New Frequency", :field-type :numeric, :backend-key :frequency}])

(defcommand request-report
  "Report"
  "Requests a report containing all sensor data and faults."
  :report
  [])

(defcommand request-imu-data
  "IMU Data"
  "Requests a report containing data from the IMU."
  :imu
  [])

(defcategory
  acs
  ::acs
  "ACS"
  [request-imu-data])

(defcategory
  battery
  ::battery
  "Battery"
  [])

(defcategory
  mission
  ::mission
  "Mission Control"
  [request-report change-sr])

(defcategory
  faults
  ::faults
  "Faults"
  [])

(def ^:const all-commands
  [acs
   battery
   mission
   faults])

(defn- filter-category-commands [category filter-str]
  (let [commands (:commands category)
        filtered (filter
                   #(or
                      (clojure.string/includes? (str/lower-case (:title %)) (str/lower-case filter-str))
                      (clojure.string/includes? (str/lower-case (:description %)) (str/lower-case filter-str)))
                   commands)]
    (assoc category :commands (vec filtered))))

(defn get-filtered-commands [filter]
  (if (or (nil? filter) (= filter ""))
    all-commands
    (map #(filter-category-commands % filter) all-commands)))

;;For now we only have one type, which is numeric 1-99. Later, might want to alter this to account for different ones
(defn get-field-validation [field-type]
  #"^(|[0-9][0-9]?)$")

(defn get-validation-tooltip [field-type]
  "0-99")

(defn check-field-nonempty [str-or-nil]
  (and str-or-nil (not= "" str-or-nil)))