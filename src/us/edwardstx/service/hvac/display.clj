(ns us.edwardstx.service.hvac.display
  (:require [com.stuartsierra.component   :as component])
  (:import [com.pi4j.component.lcd.impl I2CLcdDisplay]
           [java.time.format DateTimeFormatter]
           [java.time LocalDateTime]))

(defn space-pad [l s]
  (loop [p s]
    (if (>= (count p) l)
      p
      (recur (str p " ")))))

(defn show [{:keys [lcd]} lines]
  (loop [lines lines
         line-number 0]
    (if (not (empty? lines))
      (do
        (.write lcd line-number 0 (space-pad 20 (first lines)))
        (recur (rest lines) (+ 1 line-number))))))

(defrecord Display [lcd]
  component/Lifecycle

  (start [this]
    (assoc this :lcd (I2CLcdDisplay. 4 20 1 0x27 3 0 1 2 7 6 5 4 )))

  (stop [this]
    (assoc this :lcd nil))
  )

(defn new-display []
  (map->Display {}))
