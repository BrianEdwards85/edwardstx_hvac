(ns us.edwardstx.service.hvac.temp
  (:require [com.stuartsierra.component   :as component]
            [manifold.stream :as s]
            [clojure.string :as str]))

(def temp-file "/sys/bus/w1/devices/28-80000002cec1/w1_slave")

(defn get-temp []
  (->
   (let [r (slurp temp-file)
         i (str/index-of r "t=")]
     (.substring r (+ i 2)))
   str/trim-newline
   Integer/parseInt
   (* 9)
   (/ 5000.0)
   (+ 32)))

(defrecord Temp [stream period]
  component/Lifecycle

  (start [this]
    (assoc this :stream (s/periodically period get-temp)))

  (stop [this]
    (s/close! stream)
    (assoc this :stream nil)))

(defn new-temp [p]
  (map->Temp {:period p}))

