(ns ^:figwheel-hooks sine.dev
(:require [sine.core :as app]))

(enable-console-print!)

(defn ^:after-load render-on-reload []
  (prn "Reloaded")
  (app/render))
