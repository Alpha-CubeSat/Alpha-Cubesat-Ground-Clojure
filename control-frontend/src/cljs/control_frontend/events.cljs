(ns control-frontend.events
  (:require
    [re-frame.core :as re-frame]
    [control-frontend.db :as db]
    [control-frontend.util.http :refer [token]]
    [ajax.core :as http]
    [day8.re-frame.http-fx]
    [cljs-time.core :as time]
    [goog.crypt.base64 :as b64]))

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
  (fn [{:keys [db]} [_ type fields]]
    {:http-xhrio {:method          :post
                  :uri             "/api/cubesat/control"
                  :headers         {:authorization (token (get-in db [:control-auth :token]))}
                  :params          {:type   type
                                    :fields fields}
                  :format          (http/json-request-format)
                  :response-format (http/json-response-format {:keywords? true})
                  :on-success      [:command-submit-success type]
                  :on-failure      [:command-submit-fail type]}}))

(re-frame/reg-event-fx
  :command-submit-success
  (fn [{:keys [db]} [_ type]]
    (js/console.log "command submitted successfully")
    (let [history (-> db :commands :history)
          log {:status    :success
               :name      type
               :submitted (str (time/now))
               :message   ""}
          new-history (conj history log)
          truncated (if (> (count new-history) 15) (pop new-history) new-history)]
      {:db (assoc-in db [:commands :history] truncated)})))

(re-frame/reg-event-fx
  :command-submit-fail
  (fn [{:keys [db]} [_ type resp]]
    (js/console.log "command submission failed")
    (let [history (-> db :commands :history)
          log {:status    :failure
               :name      type
               :submitted (str (time/now))
               :message   (str "[" (:status resp) "] "
                               (-> resp :response :message))}
          new-history (conj history log)
          truncated (if (> (count new-history) 15) (pop new-history) new-history)]
      {:db (assoc-in db [:commands :history] truncated)})))

(re-frame/reg-event-fx
  :login-submitted
  (fn [_ [_ username password]]
    {:http-xhrio {:method          :post
                  :uri             "/api/auth/login"
                  :params          {:username username
                                    :password password}
                  :format          (http/json-request-format)
                  :response-format (http/json-response-format {:keywords? true})
                  :on-success      [:login-success]
                  :on-failure      [:login-failure]}}))

(re-frame/reg-event-fx
  :login-success
  (fn [{:keys [db]} [_ {token :token}]]
    (js/console.log (str token " authenticated successfully"))
    {:db (assoc-in db [:control-auth :token] token)}))

(re-frame/reg-event-fx
  :login-failure
  (fn [_ _]
    (js/console.log "authentication failed")
    nil))

(re-frame/reg-event-fx
  :change-image-selection
  (fn [{:keys [db]} [_ selected-id]]
    {:http-xhrio {:method :get
                  :uri    (str "api/cubesat/img/recent/" selected-id)
                  :response-format (http/ring-response-format)
                  :headers         {:authorization (token (get-in db [:control-auth :token]))}
                  :on-success [:image-success selected-id]
                  :on-failure [:image-failure]}}))

(re-frame/reg-event-fx
  :image-success
  (fn [{:keys [db]} [_ selected-id resp]]
    (js/console.log (str token " image successfully??" resp))
    {:db (assoc-in db [:cs-image] {:id   selected-id
                                   :data (js/btoa (js/decodeURI (js/encodeURIComponent (:body resp))))})}))

(re-frame/reg-event-fx
  :image-failure
  (fn [_ _]
    (js/console.log "image failed??")
    nil))