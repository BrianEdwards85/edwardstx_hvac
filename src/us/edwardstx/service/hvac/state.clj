(ns us.edwardstx.service.hvac.state
  (:require [com.stuartsierra.component   :as component]
            [manifold.stream :as s]
            [us.edwardstx.common.events :refer [publish-event]]
            [us.edwardstx.service.hvac.transition :refer [get-new-status]]))

(defn publish-state [{:keys [streams events]} st]
  (publish-event events "status" st)
  (doall (map
          #(s/put! % st)
          @streams))
  st)

(defn update-state [{:keys [state-map] :as state} k v]
  (swap! state-map #(let [new-status (assoc % k v)]
                      (publish-state state
                                     (assoc new-status
                                            :status (get-new-status new-status))))))

(defn add-stream [{:keys [streams]}]
  (let [s (s/stream)]
    (swap! streams #(conj % s))
    s))

(defrecord State [events state-map streams]
  component/Lifecycle

  (start [this]
    (assoc this
           :streams (atom (list))
           :state-map (atom {:mode :off
                             :temp 72
                             :status :off})))

  (stop [this]
    this))

(defn new-state []
  (component/using
   (map->State {})
   [:events]))
