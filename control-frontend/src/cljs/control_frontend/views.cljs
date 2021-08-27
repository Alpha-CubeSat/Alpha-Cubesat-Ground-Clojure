(ns control-frontend.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as ui]
   [control-frontend.subs :as subs]
   [control-frontend.commands :as commands]
   [reagent.core :as reagent]
   [ajax.core :as http]))

;; <editor-fold desc="common">
(defn widget-card [{:keys [title content]}]
  [ui/border
   :size "1 1 auto"
   :border "0px solid gray"
   :style {:margin     "10px"
           ;:padding    "10px"
           :box-shadow "2px 2px 5px"}
   :child [ui/v-box
           :size "1 1 auto"
           :children [[ui/box
                       :style {:background-color "#2b2c2e"}
                       :child [ui/title
                               :style {:color       "lightgray"
                                       :margin-left 5}
                               :level :level3
                               :label title]]
                      content]]])
;; </editor-fold>

;; <editor-fold desc="command palette">
(defn command-card-2 [{:keys [title]}]
  [ui/button
   :label [ui/title :label title :level :level3]
   :on-click #()
   :style {:color            "#000000"
           :background-color "#7c91de"
           :width            "200px"
           :height           "35px"
           :border           "0px"
           :font-size        "14px"
           :font-weight      "500"
           :box-shadow       "3px 3px 2px grey"}])

(defn command-card [{title :title :as command}]
  (let [hovered (reagent/atom {:state false})]
    (fn []
      [ui/label
       :label title
       :style {:width            "100%"
               :margin-top       "-1px"
               :background-color (if (:state @hovered) "lightgray" "transparent")}
       :attr {:on-mouse-over #(reset! hovered {:state true})
              :on-mouse-out  #(reset! hovered {:state false})
              :on-click      #(re-frame/dispatch [:change-command-selection command :new-command])}])))

(defn command-category [{:keys [title commands]}]
  [ui/v-box
   :children [[ui/title
               :label title
               :level :level3]
              [ui/line]
              [ui/v-box
               :width "160px"
               :style {:margin-top  "5px"
                       :user-select "none"}
               :children (for [command commands]
                           ^{:key command} [command-card command])]]])

(defn command-selector []
  (let [search-filter (re-frame/subscribe [:command-filter])
        all-commands (commands/get-filtered-commands @search-filter)]
    [ui/v-box
     :size "1 1 auto"
     :children [[ui/scroller
                 :v-scroll :auto
                 :child [ui/v-box
                         :gap "5px"
                         :align :center
                         :children (for [category all-commands]
                                     ^{:key category} (command-category category))]]]]))

(defn command-search []
  (let [search-filter (re-frame/subscribe [:command-filter])]
    [ui/v-box
     :children [[ui/label :label "Filter Commands"
                 :style {:margin-top "5px"
                         :font-size  "12px"}]
                [ui/input-text
                 :model @search-filter
                 :width "180px"
                 :placeholder "Start typing to search..."
                 :on-change #(re-frame/dispatch [:change-command-filter %])]]]))

(defn command-palette []
  [ui/v-box
   :align :center
   :size "1 1 auto"
   :gap "10px"
   :children [[command-search]
              [command-selector]]])

(defn command-palette-container []
  [ui/v-box
   :max-width "200px"
   :size "1 0 auto"
   :style {:background-color "#fcfcfc"}
   :children [[ui/title
               :label "Commands"
               :level :level3
               :style {:margin-left "5px"
                       :margin-top  "5px"}]
              [command-palette]]])
;; </editor-fold>

; <editor-fold desc="command viewer">

(defn command-field-1 [{:keys [title backend-key]} responses]
  (let [input (reagent/atom nil)
        status (reagent/atom :error)]
    (fn []
      [ui/v-box
       :children [[ui/label
                   :label title
                   :style {:font-size "12px"}]
                  [ui/input-text
                   :width "100%"
                   :model input
                  ;;  :placeholder (commands/get-validation-tooltip field-type)
                   :change-on-blur? false
                   :status-icon? true
                   :status @status
                   :status-tooltip "Field is mandatory"
                  ;;  :validation-regex (commands/get-field-validation field-type)
                   :on-change (fn [value]
                                (swap! responses assoc backend-key value)
                                (reset! status (if (commands/check-field-nonempty value) :success :error))
                                (reset! input value))]]])))

(defn command-field-2 [{:keys [title1 backend-key1 title2 backend-key2]} responses]
  (let [input1 (reagent/atom nil)
        input2 (reagent/atom nil)
        status1 (reagent/atom :error)
        status2 (reagent/atom :error)]
    (fn []
      [ui/v-box
       :children [[ui/label
                   :label title1
                   :style {:font-size "12px"}]
                  [ui/input-text
                   :width "100%"
                   :model input1
                  ;;  :placeholder (commands/get-validation-tooltip field-type)
                   :change-on-blur? false
                   :status-icon? true
                   :status @status1
                   :status-tooltip "Field is mandatory"
                  ;;  :validation-regex (commands/get-field-validation field-type)
                   :on-change (fn [value]
                                (swap! responses assoc backend-key1 value)
                                (reset! status1 (if (commands/check-field-nonempty value) :success :error))
                                (reset! input1 value))]
                  [ui/label
                   :label title2
                   :style {:font-size "12px"}]
                  [ui/input-text
                   :width "100%"
                   :model input2
                  ;;  :placeholder (commands/get-validation-tooltip field-type)
                   :change-on-blur? false
                   :status-icon? true
                   :status @status2
                   :status-tooltip "Field is mandatory"
                  ;;  :validation-regex (commands/get-field-validation field-type)
                   :on-change (fn [value]
                                (swap! responses assoc backend-key2 value)
                                (reset! status2 (if (commands/check-field-nonempty value) :success :error))
                                (reset! input2 value))]]])))

(defn command-args [fields responses]
  [ui/v-box
   :size "1 0 auto"
   :children [[ui/title
               :level :level3
               :label "Fields"]
              [ui/v-box
               :gap "5px"
               :children (for [field fields]
                           (if (< (count field) 3)
                             [command-field-1 field responses] [command-field-2 field responses]))]]])

(defn command-description [text]
  [ui/v-box
   :size "1 1 auto"
   :width "470px"
   :children [[ui/title
               :level :level3
               :label "Description"]
              [ui/p
               {:style {:width "100%" :min-width "200px"}}
               text]]])

(defn command-actions [responses comm-type]
  [ui/v-box
   :gap "10px"
   :children [[ui/title
               :level :level3
               :style {:margin-bottom "-5px"}
               :label "Actions"]
              ;[ui/h-box
              ; :gap "10px"
              ; :align :baseline
              ; :children [[ui/label
              ;             :label "Run after:"]
              ;            [ui/input-time
              ;             :model 900
              ;             :on-change #()]
              ;            [ui/button
              ;             :style {:width            "150px"
              ;                     :background-color "#00C851"
              ;                     :color            "#f0f0f0"}
              ;             :label "Schedule"]]]
              [ui/button
               :style {:width            "100%"
                       :background-color "#00C851"
                       :color            "#f0f0f0"}
               :label "Submit"
               :on-click #(re-frame/dispatch [:submit-command comm-type @responses])]
              [ui/button
               :style {:width            "100%"
                       :background-color "#ff4444"
                       :color            "#f0f0f0"}
               :label "Clear"]]])

(defn command-form [{:keys [title description fields backend-type]}]
  (let [responses (atom {})]
    [ui/v-box
     :size "1 1 auto"
     :style {:margin-top -10}
     :children [[ui/title
                 :level :level2
                 :label title]
                [ui/line]
                [ui/h-box
                 :size "1 1 auto"
                 :gap "10px"
                 :children [[command-description description]
                            [ui/v-box
                             :size "1 1 auto"
                             :min-width "200px"
                             :gap "15px"
                             :children [[command-args fields responses]
                                        [command-actions responses backend-type]]]]]]]))

(defn command-viewer []
  (let [selected-command (re-frame/subscribe [:command-selection])
        comm (:command @selected-command)]
    [widget-card
     {:title   "Command Selection"
      :content [ui/box
                :size "1 1 auto"
                :style {:margin  "5px"
                        :padding "10px"}
                :child [command-form comm]]}]))
; </editor-fold>

; <editor-fold desc="top bar">
(defn top-bar-image-button [image-uri]
  (let [hover (reagent/atom false)]
    (fn []
      [ui/box
       :height "50px"
       :width "50px"
       :style {:padding          "5px"
               :background-color (if @hover "#1b1c1e" "#2b2c2e")}
       :attr {:on-mouse-over #(reset! hover true)
              :on-mouse-out  #(reset! hover false)}
       :child [:img {:src image-uri}]])))

(defn top-bar [username]
  [ui/h-box
   :height "50px"
   :style {:background-color "#2b2c2e"}
   :children [[top-bar-image-button "ssa.png"]
              [top-bar-image-button "kibana.png"]
              [ui/box                                       ;Fills up the horizontal space so we can right-justify stuff
               :size "1 1 auto"
               :child [:p ""]]
              [ui/label
               :style {:padding-top   "15px"
                       :padding-right "5px"
                       :font-size     15
                       :color         "#B5B5B5"}
               :label username]
              [top-bar-image-button "user1.png"]]])
; </editor-fold>

; <editor-fold desc="login popup">
(defn login-panel []
  (let [error-msg (re-frame/subscribe [:auth-error])
        response (reagent/atom nil)]
    (fn []
      [ui/modal-panel
       :backdrop-color "grey"
       :backdrop-opacity 0.7
       :style {:font-family "Consolas"}
       :child [ui/v-box
               :gap "13px"
               :children [[:center
                           [:img {:src        "ssa.png"
                                  :align-self "center"
                                  :style      {:height 200
                                               :width  200}}]]
                          [ui/title
                           :level :level2
                           :label "Authentication"
                           :style {:margin "0"}]
                          [ui/title
                           :level :level4
                           :label "Cubesat control services require authentication with the ground system. Please sign in."
                           :style {:max-width  "250px"
                                   :margin-top "-7px"}]
                          [ui/line]
                          [ui/p
                           {:style {:margin-bottom "-22px"
                                    :width         "250px"
                                    :min-width     "250px"}}
                           "Username"]
                          [ui/input-text
                           :model (:username @response)
                           :change-on-blur? false
                           :placeholder "Enter Username"
                           :width "100%"
                           :on-change #(swap! response assoc :username %)]
                          [ui/p
                           {:style {:margin-bottom "-22px"
                                    :width         "250px"
                                    :min-width     "250px"}}
                           "Password"]
                          [ui/input-text
                           :model (:password @response)
                           :change-on-blur? false
                           :placeholder "Enter Password"
                           :width "100%"
                           :attr {:id "pf-password" :type "password"}
                           :on-change #(swap! response assoc :password %)]
                          [ui/line]
                          [ui/label
                           :style {:color "red"}
                           :label @error-msg]
                          [ui/button
                           :label "Sign in"
                           :disabled? (not
                                       (and
                                        (commands/check-field-nonempty (:username @response))
                                        (commands/check-field-nonempty (:password @response))))
                           :on-click #(re-frame/dispatch [:login-submitted (:username @response) (:password @response)])
                           :style {:align-self       "flex-end"
                                   :color            "#fff"
                                   :background-color "#337ab7"
                                   :border-color     "#2e6da4"}]]]])))
;</editor-fold>

; <editor-fold desc="command log">

(defn table-header []
  [ui/h-box
   :size "1 1 auto"
   :max-height "30px"
   :width "100%"
   :style {:background "#e8e8e8"}
   :children [[ui/label
               :style {:font-weight "bold" :padding "5px" :width "50px" :background "#e8e8e8"}
               :label "Status"]
              [ui/label
               :style {:font-weight "bold" :padding "5px" :width "140px" :background "#e8e8e8"}
               :label "Name"]
              [ui/label
               :style {:font-weight "bold" :padding "5px" :width "151px" :background "#e8e8e8"}
               :label "Submitted"]
              [ui/label
               :style {:font-weight "bold" :padding "5px" :min-width "350px" :background "#e8e8e8"}
               :label "Message"]]])

(defn row [status name submitted message]
  [ui/h-box
   :width "100%"
   :children [[ui/label
               :style {:padding "5px" :width "50px"}
               :label status]
              [ui/label
               :style {:padding "5px" :width "140px"}
               :label name]
              [ui/label
               :style {:padding "5px" :width "151px"}
               :label submitted]
              [ui/box
               :size "1 1 auto"
               :child [ui/label
                       :style {:padding "5px" :max-width "350px" :overflow "hidden"}
                       :label message]]]])

(def uniqkey (atom 0))

(defn gen-key []
  (let [res (swap! uniqkey inc)]
    res))

(defn command-history-table [commands]
  [ui/v-box
   :children [[table-header]
              (for [{:keys [status name submitted message]} commands]
                ^{:key (gen-key)} [row status name submitted message])]])

(defn command-log []
  (let [comm-history (re-frame/subscribe [:command-history])]
    [widget-card
     {:title   "Command History"
      :content [ui/scroller
                :v-scroll :auto
                :h-scroll :auto
                :child [command-history-table @comm-history]]}]))

; </editor-fold>

;; <editor-fold desc="image viewer">
(defn img-card [name]
  (let [hovered (reagent/atom {:state false})]
    (fn []
      [ui/label
       :label name
       :style {:width            "100%"
               :margin-top       "-1px"
               :padding-left     "5px"
               :background-color (if (:state @hovered) "lightgray" "transparent")}
       :attr {:on-mouse-over #(reset! hovered {:state true})
              :on-mouse-out  #(reset! hovered {:state false})
              :on-click      #(re-frame/dispatch [:change-image-selection name])}])))

(defn img-chooser []
  (let [image-names @(re-frame/subscribe [:image-names])]
    [ui/v-box
     :max-width "120px"
     :min-height "100%"
     :size "1 0 auto"
     :style {:background-color "#f1f1f1"}
     :children [[ui/label
                 :style {:font-weight "bold" :padding "5px" :min-width "120px" :background "#D2D2D2"}
                 :label "Select Image"]
                (for [name image-names]
                  [img-card name])]]))

(defn img-display []
  (let [image-data @(re-frame/subscribe [:cubesat-image])]
    (when image-data
      (print image-data)
      [:img {:src (str "data:image/jpeg;base64, " (:base64 image-data))
             :style {:width "100%"
                     :height "100%"}}])))

(defn image-viewer []
  [widget-card
   {:title   "Cubesat Images"
    :content [ui/h-box
              :size "1 1 auto"
              :children [[img-chooser]
                         [img-display]]]}])
;; </editor-fold>

;; fragment information start (not used )

(defn img-fragment-header []
  [ui/h-box
   :size "1 1 auto"
   :max-height "30px"
   :width "100%"
   :style {:background "#e8e8e8"}
   :children [[ui/label
               :style {:font-weight "bold" :padding "5px" :width "90px" :background "#e8e8e8"}
               :label "Image SN"]
              [ui/label
               :style {:font-weight "bold" :padding "5px" :width "150px" :background "#e8e8e8"}
               :label "Lastest Fragment"]
              [ui/label
               :style {:font-weight "bold" :padding "5px" :width "150px" :background "#e8e8e8"}
               :label "Fragment Count"]
              [ui/label
               :style {:font-weight "bold" :padding "5px" :min-width "200px" :background "#e8e8e8"}
               :label "Missing Fragments"]]])

(defn downlink-info []
  [widget-card
   {:title   "Downlinked Information"
    :content [ui/v-box
              :children [[img-fragment-header]]]}])

;; fragment information end

(defn center-container []
  [ui/v-box
   :size "1 1 auto"
   :max-width "900px"
   :children [[command-viewer]
              [image-viewer]]])

(defn right-container []
  [ui/v-box
   :size "1 1 auto"
   :max-width "900px"
   :children [[command-log]
              [downlink-info]]])

(defn main-container []
  [ui/h-box
   :size "1 1 auto"
   :children [[command-palette-container]
              [center-container]
              [command-log]]])

(defn main-panel []
  (let [authentication (re-frame/subscribe [:auth-token])]
    [ui/v-box
     :max-height "100vh"
     :height "100vh"
     :children [[top-bar (:username @authentication)]
                [main-container]
                (when-not (:token @authentication)
                  [login-panel])]]))
