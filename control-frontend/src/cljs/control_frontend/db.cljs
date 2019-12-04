(ns control-frontend.db)

(def default-db
  {:name "re-frame"
   :commands {:filter ""
              :selection {:selection-type :default-no-selection
                          :command nil}}})
