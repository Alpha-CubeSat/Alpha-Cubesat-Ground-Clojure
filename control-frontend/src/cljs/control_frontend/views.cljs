(ns control-frontend.views
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :as ui]
    [control-frontend.subs :as subs]
    [control-frontend.commands :as commands]))

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
   :width "230px"
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


(defn command-palette []
  (let [all-commands commands/all-commands]
    [ui/scroller
     :v-scroll :auto
     :max-width "270px"
     :child [ui/v-box
             :width "250px"
             :gap "15px"
             :align :center
             :children (into [] (for [category all-commands]
                                  (command-category category)))]]))

(defn main-panel []
  [ui/h-box
   :height "100vh"
   :children [[command-palette]]])
