(ns us.edwardstx.service.hvac.state
  (:require [com.stuartsierra.component   :as component]
            [manifold.stream :as s]
            [us.edwardstx.common.events :refer [publish-event]]
            [us.edwardstx.service.hvac.transition :refer [get-new-status]]))

(defn publish-state [{:keys [streams]} st]
  (doall (map
          #(s/put! % st)
          @streams))
  st)

(defn get-age [st]
  (let [current (.getTime (java.util.Date.))
        remote (-> st :remote :timestamp .getTime)]
    (quot (- current remote) 1000)))

(defn expire-remote [st]
  (cond
    (nil? (:remote st)) st
    (or (-> st :remote :timestamp nil?)
        (< 90 (get-age st))) (dissoc st :remote)
    :else st))

(defn update-state [{:keys [state-map events] :as state} k v]
  (swap! state-map #(let [new-status (expire-remote (assoc % k v))
                          updated-state (assoc new-status :status (get-new-status new-status))]
                      (if (not= :remote k)
                        (publish-event events "status" (select-keys updated-state [:mode :last :temp :status])))
                      (publish-state state updated-state))))

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
