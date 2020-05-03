(ns sine.core
  (:require [dumdom.core :as dumdom :refer [defcomponent]]
            [cljs.core.async :refer [<! alts! put! timeout chan]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def one-eighty-over-pi (/ 180 js/Math.PI))

(defn radians-to-degree [radians]
  ;; x degrees * rad * 180/π
  (* radians one-eighty-over-pi))

(defn to-css [rules]
  (->> rules (map
               (fn [[rule-name config]]
                 (str rule-name " { \n"
                      (->> config (map (fn [[attr val]] (str "  " (name attr) ": " val ";"))) (clojure.string/join "\n"))
                      "\n}\n")))
       (clojure.string/join "\n")))

(defcomponent styling []
  [:style {}
   (to-css {"#app" {:height "100vh"
                    :position "absolute"}
            ".container" {:width "400px" :height "400px" :position "relative" :overflow "hidden"}
            ".letter" {:display "inline-block" :position "absolute" :font-size "2rem" :font-family "Helvetica Neue" :font-weight "bold"}})])


(defcomponent Letter [{:keys [text top left rotation color]}]
  [:div {:className "letter"
         :style {:top top
                 :left left
                 :color (or color "red")
                 :transform (str "rotate(" rotation ")")}} text])

(defcomponent Main [{:keys [letters]}]
  [:div
   (styling)
   (into [:div {:className "container"}]
     (for [c letters]
       [Letter c]))])

(defonce state (atom {:letters (seq "KODEMAKER KODEMAKER KODEMAKER")
                      :tick 0
                      :amplitude 10
                      :y-offset 50
                      :x-spacing 7
                      :x-speed 0.3
                      :y-speed 0.05
                      :rotation-multiplier 0.5}))

(def colors ["#330000" "#990000" "#cc0000" "#dd0000"])

(defn y-pos [{:keys [x amplitude y-offset]}]
  (-> (js/Math.sin x)
      (* amplitude)
      (+ y-offset)))

(defn x-pos [{:keys [x width]}]
  (mod x width))

(defn prepare [state]
  {:letters (map-indexed (fn [idx letter]
                           (let [x 3])
                           {:text letter
                            :top (-> (y-pos {:x (+ (* (:y-speed state) (:tick state))
                                                   (* idx 0.5))
                                             :amplitude (:amplitude state)
                                             :y-offset (:y-offset state)})
                                     (str "%"))
                            :color (nth (cycle colors) idx)
                            :left (-> (x-pos {:x (- (* idx (:x-spacing state))
                                                    (* (:x-speed state) (:tick state)))
                                              :width 300})
                                      (str "%"))
                            :rotation (-> (js/Math.cos (+ (* (:y-speed state) (:tick state))
                                                          (* idx 0.5)))
                                          (* (:rotation-multiplier state))
                                          (radians-to-degree)
                                          (str "deg"))})
              (:letters state))})



(defn render []
  (dumdom/render (Main (prepare @state)) (js/document.getElementById "app")))

(defn render-loop []
  (render)
  (swap! state update :tick inc)
  (.requestAnimationFrame js/window render-loop))

(defonce start
  (render-loop))


(comment
  (swap! state assoc :offset 20)
  (swap! state assoc :amplitude 30))