(ns control-frontend.commands
  (:require [clojure.string :as str]
            [re-com.core :as ui])
  (:require-macros [control-frontend.command-macros :refer [defcommand defcategory]]))

(defcommand mission-mode-standby
  "Mode: Standby"
  "Sends command [mission::mode_standby]"
  :mission-mode-standby
  [])

(defcommand mission-mode-high-altitude
  "Mode: High-Altitude"
  "Sends command [mission::mode_high_altitude]"
  :mission-mode-high-altitude
  [])

(defcommand mission-mode-deployment
  "Mode: Deployment"
  "Sends command [mission::mode_deployment]"
  :mission-mode-deployment
  [])

(defcommand mission-mode-post-deployment
  "Mode: Post-Deployment"
  "Sends command [mission::mode_post_deployment]"
  :mission-mode-post-deployment
  [])

(defcommand rockblock-downlink-period
  "Downlink Period"
  "Sends command [rockblock::downlink_period] with the specified seconds"
  :rockblock-downlink-period
  [{:title "Downlink Period", :backend-key :downlink-period}])

(defcommand request-img-fragment
  "Request Image Fragment"
  "Requests the specified image fragment from the specified camera"
  :request-img-fragment
  [{:title1 "Camera Serial Number", :backend-key1 :camera-number, :title2 "Image Fragment Number", :backend-key2 :img-fragment}])

(defcategory
  mission
  ::mission
  "Mission Control"
  [mission-mode-standby mission-mode-high-altitude mission-mode-deployment mission-mode-post-deployment])

(defcategory
  rockblock
  ::rockblock
  "Rockblock"
  [rockblock-downlink-period])

(defcategory
  camera
  ::camera
  "Camera"
  [request-img-fragment])

(def ^:const all-commands
  [mission
   rockblock
   camera])

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