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
            ".button" {:padding "6px" :font-size "1.5rem" :cursor "pointer" :border "1px solid black" :display "inline-block"
                       :min-width "40px" :height "44px" :user-select "none"  :border-radius "2px" :background-color "lightgray"}
            ".letter" {:display "inline-block" :position "absolute" :font-size "2rem" :font-family "Helvetica Neue" :font-weight "bold"}})])


(defcomponent Letter [{:keys [text top left rotation color]}]
  [:div {:className "letter"
         :style {:top top
                 :left left
                 :color (or color "red")
                 :transform (str "rotate(" rotation ")")}} text])

(defcomponent Main [{:keys [letters buttons]}]
  [:div
   (styling)
   (into [:div {:className "container"}]
     (for [c letters]
       [Letter c]))
   (into [:div {:style {:display "flex"}}]
     (for [b buttons]
       (if (:on-click b)
         [:div {:style {:align-self "flex-end" :padding "4px"}}
          [:button {:className "button" :onclick (:on-click b)} (:text b)]]

         [:div {:style {:padding "4px"}}
          [:div {:style {:text-align "center" :padding "2px"}} (:text b)]
          [:button {:className "button"
                    :onclick (:inc-fn b)} "+"]
          [:button {:className "button"
                    :onclick (:dec-fn b)} "-"]])))])

(def initial-state {:letters (seq "KODEMAKER KODEMAKER KODEMAKER")
                    :tick 0
                    :amplitude 10
                    :y-offset 50
                    :x-spacing 7
                    :x-speed 0.3
                    :y-speed 0.05
                    :rotation-multiplier 0.5})

(defonce state (atom initial-state))

(def colors ["#330000" "#990000" "#cc0000" "#dd0000"])

(defn y-pos [{:keys [x amplitude y-offset]}]
  (-> (js/Math.sin x)
      (* amplitude)
      (+ y-offset)))

(defn x-pos [{:keys [x width]}]
  (mod x width))

(defn prepare [state]
  (let [{:keys [letters
                tick
                amplitude
                y-speed
                y-offset
                x-spacing
                x-speed
                rotation-multiplier]} @state]
    {:buttons [{:text "Amplitude"
                :inc-fn #(swap! state update :amplitude inc)
                :dec-fn #(swap! state update :amplitude dec)}
               {:text "X spacing"
                :inc-fn #(swap! state update :x-spacing inc)
                :dec-fn #(swap! state update :x-spacing dec)}
               {:text "Y speed"
                :inc-fn #(swap! state update :y-speed (fn [y] (+ y 0.01)))
                :dec-fn #(swap! state update :y-speed (fn [y] (max (- y 0.01) 0)))}
               {:text "X speed"
                :inc-fn #(swap! state update :x-speed (fn [x] (+ x 0.01)))
                :dec-fn #(swap! state update :x-speed (fn [x] (- x 0.01)))}
               {:text "Reset"
                :on-click #(reset! state initial-state)}]
     :letters (map-indexed (fn [idx letter]
                             (let [x (+ (* y-speed tick)
                                        (* idx 0.5))]
                               {:text letter
                                :color (nth (cycle colors) idx)
                                :top (-> (y-pos {:x x
                                                 :amplitude amplitude
                                                 :y-offset y-offset})
                                         (str "%"))
                                :left (-> (x-pos {:x (- (* idx x-spacing)
                                                        (* x-speed tick))
                                                  :width 300})
                                          (str "%"))
                                :rotation (-> (js/Math.cos x)
                                              (* rotation-multiplier)
                                              (radians-to-degree)
                                              (str "deg"))}))
                letters)}))


(defn render []
  (dumdom/render (Main (prepare state)) (js/document.getElementById "app")))

(defn render-loop []
  (render)
  (swap! state update :tick inc)
  (.requestAnimationFrame js/window render-loop))

(defonce start
  (render-loop))


(comment
  (swap! state assoc :offset 20)
  (swap! state assoc :amplitude 30))