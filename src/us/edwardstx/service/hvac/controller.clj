(ns us.edwardstx.service.hvac.controller
  (:import [com.pi4j.wiringpi Gpio]))

(def status-map {:off   {23 1, 22 1, 27 1, 17 1}
                 :fan   {23 0, 22 1, 27 1, 17 1}
                 :eheat {23 0, 22 0, 27 1, 17 1}
                 :cool  {23 0, 22 1, 27 0, 17 1}
                 :heat  {23 0, 22 1, 27 0, 17 0}})

(defn set-status [{:keys [status]}]
  (doall
   (map
    (fn [[p v]] (Gpio/digitalWrite p v))
    (sort-by first
             (get status-map status)))))
