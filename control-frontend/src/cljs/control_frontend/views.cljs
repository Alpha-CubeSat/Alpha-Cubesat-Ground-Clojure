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

(defn command-card [{:keys [title]}]
  (let [hovered (reagent/atom {:state false})]
    (fn []
      [ui/label
       :label title
       :style {:width            "100%"
               :margin-top       "-1px"
               :background-color (if (:state @hovered) "lightgray" "transparent")}
       :attr {:on-mouse-over #(reset! hovered {:state true})
              :on-mouse-out  #(reset! hovered {:state false})}])))

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
(defn command-args []
  [ui/v-box
   :children [[ui/title
               :level :level3
               :label "Fields"]
              [ui/label
               :label "New Frequency"
               :style {:font-size "12px"}]
              [ui/input-text
               :model nil
               :placeholder "0 - 99"
               :on-change #()]
              [ui/label
               :label "Some other field"
               :style {:font-size "12px"}]
              [ui/input-text
               :model nil
               :placeholder "Input"
               :on-change #()]]])

(defn command-description []
  [ui/v-box
   :children [[ui/title
               :level :level3
               :label "Description"]
              [ui/p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse condimentum magna id libero dictum, ac sodales mi aliquam. Sed scelerisque a ipsum a efficitur. Vestibulum pharetra est fermentum varius tristique. Mauris et enim dui. In faucibus tellus bibendum, pretium neque sed, vulputate orci. Vivamus scelerisque aliquet sapien, ut eleifend erat laoreet sed. \n"]
              [ui/p "Nunc placerat libero sem, in porta lacus porta ut. In et tincidunt orci, in molestie mi. Ut eu dictum erat, id scelerisque erat. Proin non aliquet tellus, dignissim dapibus massa. Aenean leo sem, volutpat euismod nibh a, ultricies luctus turpis. Maecenas quis malesuada felis, id ullamcorper ligula. Suspendisse pellentesque diam vel tempus venenatis. Fusce dapibus dolor lectus, et vestibulum turpis gravida nec. "]]])

(defn command-form []
  [ui/v-box
   :children [[ui/title
               :level :level2
               :label "Change SR"]
              [ui/line]
              [ui/h-box
               :gap "15px"
               :children [[command-description]
                          [command-args]]]]])

(defn command-viewer []
  :size "1 1 auto"
  [ui/v-box
   :size "1 1 auto"
   :style {:margin-top  "-5px"
           :margin-left "10px"}
   :children [[ui/title
               :level :level3
               :label "Selection"]
              [command-form]]])
; </editor-fold>

; <editor-fold desc="top bar">
(defn top-bar []
  [ui/h-box
   :height "50px"
   :style {:background-color "gray"}
   :children [[:p "Top bar placeholder"]]])
; </editor-fold>

(defn center-container []
  [ui/v-box
   :size "1 1 auto"
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
