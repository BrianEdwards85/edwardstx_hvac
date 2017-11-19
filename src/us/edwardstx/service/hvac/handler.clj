(ns us.edwardstx.service.hvac.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging      :as log]
            [us.edwardstx.common.tasks :as tasks]
            [us.edwardstx.common.events :refer [publish-event] :as events]
            [us.edwardstx.service.hvac.state :as state]
            [us.edwardstx.service.hvac.controller :refer [status-map]]
            [manifold.stream :as s]
            [manifold.deferred :as d]))

(def status-set (set (keys status-map)))

(defn parseInteger [x]
  (try
    (Integer/parseInt x)
    (catch Exception e nil)))

(defn mode-handler [{:keys [state events]} {:keys [body response]}]
  (d/success! response true)
  (let [mode (keyword body)]
    (when (status-set mode)
      (log/info (format "Mode update to %s" body))
      (publish-event events "state.mode.updated" {:mode mode})
      (state/update-state state :mode mode))))

(defn temp-handler [{:keys [state events]} {:keys [body response]}]
  (d/success! response true)
  (when-let [temp (parseInteger body)]
    (log/info (format "Set temp updated to %d" temp))
    (publish-event events "state.temp.updated" {:temp temp})
    (state/update-state state :temp temp)))

(defn map-handler [{:keys [state events]} {:keys [body response]}]
  (d/success! response true)
  (if (map? body)
    (let [{:keys [mode temp]} body]
      (publish-event events "state.updated" body)
      (if (and mode (status-set mode))
        (state/update-state state :mode mode))
      (if (and temp (integer? temp))
        (state/update-state state :temp temp)))))

(defn event-handler [{:keys [state]} {:keys [body timestamp]}]
  (state/update-state state :remote (assoc body :timestamp timestamp)))

(defn create-handlers [{:keys [tasks events] :as handler}]
  (let [mode-handler-stream (tasks/task-subscription tasks "state.mode.update")
        temp-handler-stream (tasks/task-subscription tasks "state.temp.update")
        map-handler-stream (tasks/task-subscription tasks "state.update")
        event-handler-stream (events/event-subscription events "events.bedroom_sensor.reading")]
    (s/consume (partial map-handler handler) map-handler-stream)
    (s/consume (partial mode-handler handler) mode-handler-stream)
    (s/consume (partial temp-handler handler) temp-handler-stream)
    (s/consume (partial event-handler handler) event-handler-stream)
    (list mode-handler-stream temp-handler-stream map-handler-stream event-handler-stream)
    )
  )

(defrecord Handler [state tasks events streams]
  component/Lifecycle

  (start [this]
    (assoc this :streams (create-handlers this)))

  (stop [this]
    (->> this
         :streams
         (map s/close!)
         doall)
    (assoc this :streams nil)))

(defn new-handler []
  (component/using
   (map->Handler {})
   [:state :tasks :events]))
