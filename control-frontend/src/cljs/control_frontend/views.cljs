(ns control-frontend.views
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :as ui]
    [control-frontend.subs :as subs]
    [control-frontend.commands :as commands]
    [reagent.core :as reagent]
    [ajax.core :as http]))


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

(defn command-field [{:keys [title field-type backend-key]} responses]
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
                   :placeholder (commands/get-validation-tooltip field-type)
                   :change-on-blur? false
                   :status-icon? true
                   :status @status
                   :status-tooltip "Field is mandatory"
                   :validation-regex (commands/get-field-validation field-type)
                   :on-change (fn [value]
                                (swap! responses assoc backend-key value)
                                (reset! status (if (commands/check-field-nonempty value) :success :error))
                                (reset! input value))]]])))

(defn command-args [fields responses]
  [ui/v-box
   :size "1 0 auto"
   :children [[ui/title
               :level :level3
               :label "Fields"]
              [ui/v-box
               :gap "5px"
               :children (for [field fields]
                           [command-field field responses])]]])

(defn command-description [text]
  [ui/v-box
   :size "1 1 auto"
   :width "470px"
   :children [[ui/title
               :level :level3
               :label "Description"]
              [ui/scroller
               :size "1 1 auto"
               :v-scroll :auto
               :child [ui/p text]]]])

(defn command-actions [responses comm-type]
  [ui/v-box
   :gap "10px"
   :children [[ui/title
               :level :level3
               :style {:margin-bottom "-5px"}
               :label "Actions"]
              [ui/h-box
               :gap "10px"
               :align :baseline
               :children [[ui/label
                           :label "Run after:"]
                          [ui/input-time
                           :model 900
                           :on-change #()]
                          [ui/button
                           :style {:width            "150px"
                                   :background-color "#00C851"
                                   :color            "#f0f0f0"}
                           :label "Schedule"]]]
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
                            [ui/scroller
                             :v-scroll :auto
                             :size "1 1 auto"
                             :child [ui/v-box
                                     :size "1 1 auto"
                                     :gap "15px"
                                     :children [[command-args fields responses]
                                                [command-actions responses backend-type]]]]]]]]))

(defn command-viewer []
  (let [selected-command (re-frame/subscribe [:command-selection])
        comm (:command @selected-command)]
    [ui/v-box
     :size "1 1 auto"
     :style {:margin-top  "-5px"
             :margin-left "10px"}
     :children [[ui/title
                 :level :level3
                 :label "Selection"]
                (if comm [ui/border
                          :size "1 1 auto"
                          :border "0px solid gray"
                          :style {:margin     "10px"
                                  :padding    "10px"
                                  :box-shadow "2px 2px 5px"}
                          :child [command-form comm]]
                         [ui/p "No Selection"])]]))
; </editor-fold>

; <editor-fold desc="top bar">
(defn top-bar []
  [ui/h-box
   :height "50px"
   :style {:background-color "#2b2c2e"}
   :children [[:p "Top bar placeholder"]]])
; </editor-fold>

; <editor-fold desc="login popup">
(defn login-panel []
  (let [response (reagent/atom nil)]
    (fn []
      [ui/modal-panel
       :backdrop-color "grey"
       :backdrop-opacity 0.7
       :style {:font-family "Consolas"}
       :child [ui/v-box
               :gap "13px"
               :children [[ui/title
                           :level :level2
                           :label "Authentication"
                           :style {:margin "0"}]
                          [ui/title
                           :level :level4
                           :label "Cubesat control services require authentication with the ground system. Please sign in."
                           :style {:max-width "250px"
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
                          [ui/button
                           :label "Sign in"
                           :disabled? (not
                                        (and
                                          (commands/check-field-nonempty (:username @response))
                                          (commands/check-field-nonempty (:password @response))))
                           :on-click #(re-frame/dispatch [:login-submitted (:username @response) (:password @response)])
                           :style {:align-self "flex-end"
                                   :color "#fff"
                                   :background-color "#337ab7"
                                   :border-color "#2e6da4"}]]]])))
;</editor-fold>

(defn center-container []
  [ui/v-box
   :size "0 1 auto"
   :children [[command-viewer]
              [ui/box
               :height "50%"
               :size "0 0 auto"
               :child [ui/p "Command history placeholder TODO"]]]])

(defn main-container []
  [ui/h-box
   :size "1 1 auto"
   :children [[command-palette-container]
              [center-container]]])

(defn main-panel []
  (let [authentication (re-frame/subscribe [:auth-token])]
    [ui/v-box
     :max-height "100vh"
     :height "100vh"
     :children [[top-bar]
                [main-container]
                (when-not @authentication
                  [login-panel])]]))
