(ns control-frontend.events
  (:require
   [re-frame.core :as re-frame]
   [control-frontend.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame.core/reg-event-fx
  :change-command-filter
  (fn [{:keys [db]} [_ new-filter]]
    {:db (assoc-in db [:commands :filter] new-filter)}))

(re-frame.core/reg-event-fx
  :change-command-selection
  (fn [{:keys [db]} [_ new-command selection-type]]
    {:db (-> db
             (assoc-in [:commands :selection :command] new-command)
             (assoc-in [:commands :selection :selection-type] selection-type))}))