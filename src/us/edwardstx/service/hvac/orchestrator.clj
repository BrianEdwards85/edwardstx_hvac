(ns us.edwardstx.service.hvac.orchestrator
  (:require [com.stuartsierra.component   :as component]
            [manifold.stream :as s]
            [us.edwardstx.service.hvac.controller :refer [set-status]]
            [us.edwardstx.service.hvac.display :as display]
            [us.edwardstx.service.hvac.state :as state])
  (:import [java.time.format DateTimeFormatter]
           [java.time LocalDateTime]))

(def formatter (DateTimeFormatter/ofPattern "EEE dd h:mm:ss a"))

(defn start-temp [{:keys [state temp]}]
  (s/consume
   #(state/update-state state :last %)
   (:stream temp)))

(defn display-state [display st]
  (display/show display
                (list (format  "Temp: %3.1fF" (:last st))
                      (format  "Set:  %2d.0F" (:temp st))
                      (format  "Mode: %s / %s" (-> st :mode name) (-> st :status name))
                      (.format (LocalDateTime/now) formatter))))

(defn start-display [{:keys [state display]}]
  (let [stream (state/add-stream state)]
    (s/consume
     #(do
        (set-status %1)
        (display-state display %1))
     stream)
    stream))

(defrecord Orchestrator [state temp display display-stream]
  component/Lifecycle

  (start [this]
    (start-temp this)
    (assoc this :display-stream (start-display this)))


  (stop [this]
    (s/close! display-stream)
    (assoc this :display-stream nil)))

(defn new-orchestrator []
  (component/using
   (map->Orchestrator {})
   [:state :temp :display]))
