(ns control-frontend.commands
  (:require [clojure.string :as str]
            [re-com.core :as ui])
  (:require-macros [control-frontend.command-macros :refer [defcommand defcategory]]))

(comment
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
    []))

(defcommand mission-mode-low-power
  "Mode: Low Power"
  "Sends command [mission::mode_low_power]"
  :mission-mode-low-power
  [])

(defcommand mission-mode-deployment
  "Mode: Deployment"
  "Sends command [mission::mode_deployment]"
  :mission-mode-deployment
  [])

(defcommand mission-mode-standby
  "Mode: Standby"
  "Sends command [mission::mode_standby]"
  :mission-mode-standby
  [])

(defcommand mission-mode-safe
  "Mode: Safe"
  "Sends command [mission::mode_safe]"
  :mission-mode-safe
  [])

(defcommand burnwire-arm-true
  "Arm: True"
  "Sends command [burnwire::arm_true]"
  :burnwire-arm-true
  [])

(defcommand burnwire-arm-false
  "Arm: False"
  "Sends command [burnwire::arm_false]"
  :burnwire-arm-false
  [])

(defcommand burnwire-fire-true
  "Fire: True"
  "Sends command [burnwire::fire_true]"
  :burnwire-fire-true
  [])

(defcommand burnwire-fire-false
  "Fire: False"
  "Sends command [burnwire::fire_false]"
  :burnwire-fire-false
  [])

(defcommand rockblock-downlink-period
  "Downlink Period"
  "Sends command [rockblock::downlink_period] with specified seconds"
  :rockblock-downlink-period
  [])

(defcommand request-img-fragment
  "Request Image Fragment"
  "Requests the specified image fragment from the specified camera"
  :request-img-fragment
  [])

(defcommand take-photo-true
  "Take Photo: True"
  "Sends command [camera::take_photo_true]"
  :take-photo-true
  [])

(defcommand take-photo-false
  "Take Photo: False"
  "Sends command [camera::take_photo_false]"
  :take-photo-false
  [])

(defcommand temperature-mode-active
  "Mode: Active"
  "Sends command [temperature::mode_active]"
  :temperature-mode-active
  [])

(defcommand temperature-mode-inactive
  "Mode: Inactive"
  "Sends command [temperature::mode_inactive]"
  :temperature-mode-inactive
  [])

(defcommand acs-mode-detumbling
  "Mode: Detumbling"
  "Sends command [acs::mode_detumbling]"
  :acs-mode-detumbling
  [])

(defcommand acs-mode-pointing
  "Mode: Pointing"
  "Sends command [acs::mode_pointing]"
  :acs-mode-pointing
  [])

(defcommand acs-mode-off
  "Mode: Off"
  "Sends command [acs::mode_off]"
  :acs-mode-off
  [])

(defcommand fault-mode-active
  "Fault Mode: Active"
  "Sends command [fault::mode_active]"
  :fault-mode-active
  [])

(defcommand fault-mode-inactive
  "Fault Mode: Inactive"
  "Sends command [fault::mode_inactive]"
  :fault-mode-inactive
  [])

(defcommand fault-check-mag-x-true
  "Check Mag X: True"
  "Sends command [fault::check_mag_x_true]"
  :fault-check-mag-x-true
  [])

(defcommand fault-check-mag-x-false
  "Check Mag X: False"
  "Sends command [fault::check_mag_x_false]"
  :fault-check-mag-x-false
  [])

(defcommand fault-check-mag-y-true
  "Check Mag Y: True"
  "Sends command [fault::check_mag_y_true]"
  :fault-check-mag-y-true
  [])

(defcommand fault-check-mag-y-false
  "Check Mag Y: False"
  "Sends command [fault::check_mag_y_false]"
  :fault-check-mag-y-false
  [])

(defcommand fault-check-mag-z-true
  "Check Mag Z: True"
  "Sends command [fault::check_mag_z_true]"
  :fault-check-mag-z-true
  [])

(defcommand fault-check-mag-z-false
  "Check Mag Z: False"
  "Sends command [fault::check_mag_z_false]"
  :fault-check-mag-z-false
  [])

(defcommand fault-check-gyro-x-true
  "Check Gyro X: True"
  "Sends command [fault::check_gyro_x_true]"
  :fault-check-gyro-x-true
  [])

(defcommand fault-check-gyro-x-false
  "Check Gyro X: False"
  "Sends command [fault::check_gyro_x_false]"
  :fault-check-gyro-x-false
  [])

(defcommand fault-check-gyro-y-true
  "Check Gyro Y: True"
  "Sends command [fault::check_gyro_y_true]"
  :fault-check-gyro-y-true
  [])

(defcommand fault-check-gyro-y-false
  "Check Gyro Y: False"
  "Sends command [fault::check_gyro_y_false]"
  :fault-check-gyro-y-false
  [])

(defcommand fault-check-gyro-z-true
  "Check Gyro Z: True"
  "Sends command [fault::check_gyro_z_true]"
  :fault-check-gyro-z-true
  [])

(defcommand fault-check-gyro-z-false
  "Check Gyro Z: False"
  "Sends command [fault::check_gyro_z_false]"
  :fault-check-gyro-z-false
  [])

(defcommand fault-check-temp-c-true
  "Check Temp: True"
  "Sends command [fault::check_temp_c_true]"
  :fault-check-temp-c-true
  [])

(defcommand fault-check-temp-c-false
  "Check Temp: False"
  "Sends command [fault::check_temp_c_false]"
  :fault-check-temp-c-false
  [])

(defcommand fault-check-solar-true
  "Check Solar Curr: True"
  "Sends command [fault::check_solar_current_true]"
  :fault-check-solar-true
  [])

(defcommand fault-check-solar-false
  "Check Solar Curr: False"
  "Sends command [fault::check_solar_current_false]"
  :fault-check-solar-false
  [])

(defcommand fault-check-voltage-true
  "Check Voltage: True"
  "Sends command [fault::check_voltage_true]"
  :fault-check-voltage-true
  [])

(defcommand fault-check-voltage-false
  "Check Voltage: False"
  "Sends command [fault::check_voltage_false]"
  :fault-check-voltage-false
  [])

(defcategory
  acs
  ::acs
  "ACS"
  [acs-mode-detumbling acs-mode-pointing acs-mode-off])

(defcategory
  mission
  ::mission
  "Mission Control"
  [mission-mode-low-power mission-mode-deployment mission-mode-safe mission-mode-standby])

(defcategory
  faults
  ::faults
  "Faults"
  [fault-mode-active fault-mode-inactive fault-check-mag-x-true fault-check-gyro-x-false
   fault-check-mag-y-true fault-check-mag-y-false fault-check-mag-z-true fault-check-mag-z-false
   fault-check-gyro-x-true fault-check-gyro-x-false fault-check-gyro-y-true fault-check-gyro-y-false
   fault-check-gyro-z-true fault-check-gyro-z-false fault-check-temp-c-true fault-check-temp-c-false
   fault-check-solar-true fault-check-solar-false fault-check-voltage-true fault-check-voltage-false])

(defcategory
  burnwire
  ::burnwire
  "Burnwire"
  [burnwire-arm-true burnwire-arm-false burnwire-fire-true burnwire-fire-false])

(defcategory
  rockblock
  ::rockblock
  "Rockblock"
  [rockblock-downlink-period])

(defcategory
  camera
  ::camera
  "Camera"
  [request-img-fragment take-photo-true take-photo-false])

(defcategory
  temperature
  ::temperature
  "Temperature"
  [temperature-mode-active temperature-mode-inactive])

(def ^:const all-commands
  [mission
   burnwire
   rockblock
   camera
   temperature
   acs
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