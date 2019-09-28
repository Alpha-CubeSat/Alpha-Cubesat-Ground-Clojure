(ns control-frontend.events
  (:require
   [re-frame.core :as re-frame]
   [control-frontend.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
