(ns control-frontend.subs
  (:require
   [re-frame.core :as re-frame]
   [control-frontend.commands :as commands]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame.core/reg-sub
  :command-filter
  (fn [db v]
    (-> db :commands :filter)))

(re-frame.core/reg-sub
  :command-selection
  (fn [db v]
    (-> db :commands :selection)))