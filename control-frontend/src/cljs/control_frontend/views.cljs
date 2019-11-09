(ns control-frontend.views
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :as ui]
    [control-frontend.subs :as subs]
    [control-frontend.commands :as commands]
    [reagent.core :as reagent]))

;; <editor-fold desc="command palette">
(defn command-card [{:keys [title]}]
  [ui/button
   :label [ui/title :label title :level :level3]
   :on-click #()
   :style {:color            "#000000"
           :background-color "#7c91de"
           :width            "200px"
           :height           "50px"
           :border           "0px"
           :font-size        "14px"
           :font-weight      "500"
           :box-shadow       "3px 3px 2px grey"}])

(defn command-category [{:keys [title commands]}]
  [ui/v-box
   :max-width "230px"
   :children [[ui/title
               :label title
               :level :level2]
              [ui/line]
              [ui/v-box
               :width "230px"
               :gap "15px"
               :style {:margin-top "15px"}
               :children (into [] (for [command commands]
                                    (command-card command)))]]])

(defn command-selector []
  (let [search-filter (re-frame/subscribe [:command-filter])
        all-commands (commands/get-filtered-commands @search-filter)]
    [ui/v-box
     :max-width "270px"
     :size "1 1 auto"
     :children [[ui/scroller
                 :v-scroll :auto
                 :width "270px"
                 :child [ui/v-box
                         :gap "15px"
                         :style {:padding-bottom "20px"}
                         :align :center
                         :children (into [] (for [category all-commands]
                                              (command-category category)))]]]]))

(defn command-search []
  (let [search-filter (re-frame.core/subscribe [:command-filter])]
    [ui/input-text
     :model @search-filter
     :width "235px"
     :placeholder "Search..."
     :style {:margin-left "-7px"
             :margin-top  "10px"}
     :on-change #(re-frame/dispatch [:change-command-filter %])]))

(defn command-palette []
  [ui/v-box
   :align :center
   :max-width "270px"
   :size "1 1 auto"
   :gap "10px"
   :children [[command-search]
              [command-selector]]])

(defn command-palette-container []
  [ui/v-box
   :max-width "270px"
   :size "1 1 auto"
   :style {:background-color "#fcfcfc"}
   :children [[ui/title
               :label "Commands"
               :level :level3
               :style {:margin-left "5px"
                       :margin-top  "5px"}]
              [command-palette]]])
;; </editor-fold>

;; <editor-fold desc="top bar">
(defn top-bar []
  [ui/h-box
   :height "50px"
   :children [[:p "Top bar placeholder"]]])
;</editor-fold>

(defn main-container []
  [ui/h-box
   :size "1 1 auto"
   :children [[command-palette]]])

(defn main-panel []
  [ui/v-box
   :max-height "100vh"
   :height "100vh"
   :children [[top-bar]
              [command-palette-container]]])
