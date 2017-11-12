(ns us.edwardstx.hvac
  (:require [manifold.deferred :as d]
            [manifold.stream :as stream]
            [us.edwardstx.hvac.transition :as t]
            [us.edwardstx.hvac.io :as io]
            [us.edwardstx.hvac.display :as display]
            [us.edwardstx.hvac.mqtt :as mqtt]
            [clojure.tools.nrepl.server :as nrepl]
            [clojure.data.json :as json]
            [hare.core :as hare]
            [overtone.at-at :as at])
  (:import [com.pi4j.wiringpi Gpio])
  (:gen-class))

(def publish-fields #{:last :mode :temp :status})

(defonce s (atom {:mode :off
                  :temp 70
                  :lcd nil
;;                  :at (at/mk-pool)
;;                  :term (d/deferred)
;;                  :nrepl (nrepl/start-server :port 7888)
                  :status :off}))

(defn update-status [sm]
  (let [current-temp (io/get-temp)
        nm (assoc sm :last current-temp)
        ns (t/get-new-state nm)]
    (assoc nm :status ns)))

(defn update-temp [s]
  (let [ns (swap! s update-status)]
    (io/set-status (:status ns))
    (display/display-temp (:lcd ns) (:last ns) (:temp ns) (name (:mode ns)) (name (:status ns)))))

(defn parseInteger [x]
  (try
    (Integer/parseInt x)
    (catch Exception e nil)))

(defn publish-status [s]
  (let [conn (:mqtt s)
        status (-> s
                   (select-keys publish-fields)
                   json/write-str)]
    (hare/send-message conn ".hvac.status" status)
    (if (:last s) (hare/send-message conn ".hvac.status.temp" (format "%3.1fF" (:last s))))
    (if (:temp s) (hare/send-message conn ".hvac.status.set" (format "%2d.0F"(:temp s))))
    (if (:mode s) (hare/send-message conn ".hvac.status.mode" (name (:mode s))))
    (if (:status s) (hare/send-message conn ".hvac.status.status" (name (:status s))))
    ))

(defn update-set-temp [v]
  (if-let [t (parseInteger v)]
    (t/set-temp! (int t) s)
    (publish-status @s)))

(defn update-mode [v]
  (let [m (keyword v)]
    (if (get io/status m)
      (t/set-mode! m s)
      (publish-status @s))))

(defn init []
  (Gpio/wiringPiSetupSys)
  (swap! s #(assoc % :lcd (display/init-display)))
  (swap! s #(assoc % :nrepl (nrepl/start-server :port 7888)))
  (swap! s #(assoc % :at (at/mk-pool)))
  (swap! s #(assoc % :term (d/deferred)))
  (let [c (mqtt/conn (mqtt/conf))]
    (swap! s #(assoc % :mqtt c))
    (stream/consume update-mode (mqtt/subscribe c ".hvac.mode"))
    (stream/consume update-set-temp (mqtt/subscribe c ".hvac.temp")))
  (at/every 480 #(update-temp s) (:at @s))
  (at/every 60000 #(publish-status @s) (:at @s)))

(defn -main [& args]
  (init)
  (deref (:term @s)))

