(ns control-frontend.events
  (:require
   [re-frame.core :as re-frame]
   [control-frontend.db :as db]
   [ajax.core :as http]
   [day8.re-frame.http-fx]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-fx
  :change-command-filter
  (fn [{:keys [db]} [_ new-filter]]
    {:db (assoc-in db [:commands :filter] new-filter)}))

(re-frame/reg-event-fx
  :change-command-selection
  (fn [{:keys [db]} [_ new-command selection-type]]
    {:db (-> db
             (assoc-in [:commands :selection :command] new-command)
             (assoc-in [:commands :selection :selection-type] selection-type))}))

(re-frame/reg-event-fx
  :submit-command
  (fn [_ [_ type fields]]
    {:http-xhrio {:method :post
                  :uri "/api/control"
                  :params {:operation {:type type
                                       :fields fields}}
                  :format (http/json-request-format)
                  :response-format (http/json-response-format {:keywords? true})
                  :on-success [:command-submit-success]
                  :on-failure [:command-submit-fail]}}))

(re-frame/reg-event-fx
  :command-submit-success
  (fn [_ _]
    (js/console.log "command submitted successfully")
    nil))

(re-frame/reg-event-fx
  :command-submit-fail
  (fn [_ _]
    (js/console.log "command submission failed")
    nil))