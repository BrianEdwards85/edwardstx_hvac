(ns us.edwardstx.hvac.io
  (:require [clojure.string :as s])
  (:import [com.pi4j.wiringpi Gpio]))

(def status {:off   {23 1, 22 1, 27 1, 17 1}
             :fan   {23 0, 22 1, 27 1, 17 1}
             :eheat {23 0, 22 0, 27 1, 17 1}
             :cool  {23 0, 22 1, 27 0, 17 1}
             :heat  {23 0, 22 1, 27 0, 17 0}})

(def temp-file "/sys/bus/w1/devices/28-80000002cec1/w1_slave")

(defn set-status [s]
  (doall
   (map
    (fn [[p v]] (Gpio/digitalWrite p v))
    (sort-by first
             (get status s)))))

(defn get-temp []
  (->
     (let [r (slurp temp-file)
           i (s/index-of r "t=")]
       (.substring r (+ i 2)))
   s/trim-newline
   Integer/parseInt
   (* 9)
   (/ 5000.0)
   (+ 32)))
