(ns control-frontend.commands
  (:require-macros [control-frontend.command-macros :refer [defcommand defcategory]]))

(defcommand change-sr
  "Change SR"
  "Changes the SR frequency to the desired frequency."
  :change-sr
  [{:title "New Frequency", :field=type :numeric, :backend-key :frequency}])

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

(def ^:const all-commands
  [acs
   battery
   mission])