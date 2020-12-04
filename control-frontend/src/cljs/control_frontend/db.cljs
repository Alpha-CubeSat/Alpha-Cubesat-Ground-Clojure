(ns control-frontend.db)

(def default-db
  {:name         "re-frame"
   :control-auth {:token nil
                  :username "Unauthenticated"
                  :auth-error-message ""}
   :commands     {:filter    ""
                  :selection {:selection-type :default-no-selection
                              :command        nil}
                  :history   []}
   :cs-images    []
   :cs-image     nil})
