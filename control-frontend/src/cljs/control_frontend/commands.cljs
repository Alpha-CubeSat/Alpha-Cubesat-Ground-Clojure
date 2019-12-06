(ns control-frontend.commands
  (:require [clojure.string :as str])
  (:require-macros [control-frontend.command-macros :refer [defcommand defcategory]]))

(defcommand change-sr
  "Change SR"
  "Changes the SR frequency to the desired frequen cy.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency.Changes the SR frequency to the desired frequency."
  :change-sr
  [{:title "New Frequency", :field-type :numeric, :backend-key :frequency}
   {:title "Some other field", :field-type :numeric, :backend-key :frequency}])

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
  [change-sr request-report request-imu-data])

(defcategory
  battery
  ::battery
  "Battery"
  [change-sr request-report request-imu-data])

(defcategory
  mission
  ::mission
  "Mission Control"
  [change-sr request-report request-imu-data])

(defcategory
  faults
  ::faults
  "Faults"
  [change-sr request-report request-imu-data])

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