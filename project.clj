(defproject us.edwardstx/hvac "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-RC1"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cheshire "5.8.0"]
                 [yogthos/config "0.9"]
                 [org.clojure/data.json "0.2.6"]
                 [overtone/at-at "1.2.0"]
                 [com.pi4j/pi4j-device "1.1"]
                 [com.pi4j/pi4j-core "1.1"]
                 [us.edwardstx/edwardstx_common "1.0.3-SNAPSHOT"]]

  :main us.edwardstx.service.hvac

  :uberjar-name "hvac-daemon.jar"

  :profiles {:dev {:repl-options {:init-ns us.edwardstx.service.hvac
                                  :timeout 300000}

                   :dependencies [[binaryage/devtools "0.9.4"]
                                  [org.clojure/tools.nrepl "0.2.13"]]

                   :plugins [[cider/cider-nrepl "0.15.1"]]

                   :resource-paths ["env/dev/resources" "resources"]

                   :env {:dev true}}

             :uberjar {:env {:production true}
                       :aot :all
                       :omit-source true}}

)


