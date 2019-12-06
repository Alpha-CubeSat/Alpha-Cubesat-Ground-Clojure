(ns control-frontend.views
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :as ui]
    [control-frontend.subs :as subs]
    [control-frontend.commands :as commands]
    [reagent.core :as reagent]))

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
                           [command-card command])]]])

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
                                     (command-category category))]]]]))

(defn command-search []
  (let [search-filter (re-frame.core/subscribe [:command-filter])]
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
   :size "1 1 auto"
   :style {:background-color "#fcfcfc"}
   :children [[ui/title
               :label "Commands"
               :level :level3
               :style {:margin-left "5px"
                       :margin-top  "5px"}]
              [command-palette]]])
;; </editor-fold>

; <editor-fold desc="command viewer">
;TODO this is all hardcoded placeholder for now

(defn command-field [{:keys [title field-type backend-key]}]
  [ui/v-box
   :children [[ui/label
               :label title
               :style {:font-size "12px"}]
              [ui/input-text
               :width "100%"
               :model nil
               :placeholder "placeholder"
               :on-change #()]]])

(defn command-args [fields]
  [ui/v-box
   :size "1 0 auto"
   :children [[ui/title
               :level :level3
               :label "Fields"]
              [ui/v-box
               :gap "5px"
               :children (for [field fields]
                           [command-field field])]]])

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

(defn command-actions []
  [ui/v-box
   :gap "10px"
   :children [[ui/title
               :level :level3
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
                                   :background-color "lightgreen"}
                           :label "Schedule"]]]
              [ui/button
               :style {:width            "100%"
                       :background-color "lightgreen"}
               :label "Run"]
              [ui/button
               :style {:width            "100%"
                       :background-color "#d9534f"
                       :border-color     "#d43f3a"
                       :color            "lightgray"}
               :label "Clear"]]])

(defn command-form [{:keys [title description fields]}]
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
                           :gap "15px"
                           :children [[command-args fields]
                                      [command-actions]]]]]]])

(defn command-viewer []
  (let [selected-command (re-frame/subscribe [:command-selection])]
    [ui/v-box
     :size "1 1 auto"
     :style {:margin-top  "-5px"
             :margin-left "10px"}
     :children [[ui/title
                 :level :level3
                 :label "Selection"]
                [ui/border
                 :size "1 1 auto"
                 :border "0px solid gray"
                 :style {:margin     "10px"
                         :padding    "10px"
                         :box-shadow "2px 2px 5px"}
                 :child [command-form (:command @selected-command)]]]]))
; </editor-fold>

; <editor-fold desc="top bar">
(defn top-bar []
  [ui/h-box
   :height "50px"
   :style {:background-color "#2b2c2e"}
   :children [[:p "Top bar placeholder"]]])
; </editor-fold>

(defn center-container []
  [ui/v-box
   :size "0 0 auto"
   :children [[command-viewer]
              [command-viewer]]])

(defn main-container []
  [ui/h-box
   :size "1 1 auto"
   :children [[command-palette-container]
              [center-container]]])

(defn main-panel []
  [ui/v-box
   :max-height "100vh"
   :height "100vh"
   :children [[top-bar]
              [main-container]]])
