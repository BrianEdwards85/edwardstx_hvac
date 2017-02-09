(ns us.edwardstx.hvac.display
  (:import [com.pi4j.component.lcd.impl I2CLcdDisplay]
           [java.time.format DateTimeFormatter]
           [java.time LocalDateTime]))

(def formatter (DateTimeFormatter/ofPattern "EEE dd h:mm:ss a"))

(defn space-pad [l s]
  (loop [p s]
    (if (>= (count p) l)
      p
      (recur (str p " ")))))

(defn display-temp [lcd c t m s]
  (.write lcd 0 0 (space-pad 20 (format  "Temp: %3.1fF"  c)))
  (.write lcd 1 0 (space-pad 20 (format  "Set:  %2d.0F"  t)))
  (.write lcd 2 0 (space-pad 20 (format  "Mode: %s / %s" m s)))
  (.write lcd 3 0 (space-pad 20 (.format (LocalDateTime/now) formatter))))

(defn init-display []
  (I2CLcdDisplay. 4 20 1 0x27 3 0 1 2 7 6 5 4 ))
