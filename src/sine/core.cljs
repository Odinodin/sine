(ns sine.core
  (:require [dumdom.core :as dumdom :refer [defcomponent]]))

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
   (to-css {"#app" {}
            ".container" {:position "relative" :overflow "hidden"}
            ".button" {:padding "6px" :font-size "1.2rem" :cursor "pointer" :border "1px solid black" :display "inline-block"
                       :min-width "40px" :height "44px" :user-select "none" :border-radius "2px" :background-color "lightgray"}
            ".letter" {:display "inline-block" :position "absolute" :font-size "2rem" :font-family "Helvetica Neue" :font-weight "bold"}
            ".effect-shadow" {:color "#f8f8ff" :text-shadow "0 -2px 3px #111,0 3px 4px #666"}
            ".effect-blurred" {:color "transparent" :text-shadow "0 0 30px #000"}
            ".effect-background" {:color "black !important"
                                  :background "linear-gradient(60deg, #cc0000, #ff8888)"
                                  :border-radius "10px" :padding "4px"}})])

(defcomponent Letter [{:keys [text top left rotation color effect-class]}]
  [:div {:className (str "letter " effect-class)
         :style {:top top
                 :display (when (= text " ") "none")
                 :left left
                 :color (or color "red")
                 :transform (str "rotate(" rotation ")")}} text])

(defcomponent Main [{:keys [letters buttons width height]}]
  [:div
   (styling)
   (into [:div {:className "container" :style {:width width :height height}}]
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
                    :amplitude 30
                    :angle-speed 0.3
                    :x-speed -10
                    :x-spacing 26
                    :speed (/ 1 8)
                    :rotation-multiplier 0.5
                    :effect-index 0})

(defonce state (atom initial-state))

(def colors ["#641220" "#6e1423" "#85182a" "#a11d33" "#a71e34" "#b21e35" "#bd1f36" "#c71f37" "#da1e37" "#e01e37"])
(def effects ["effect-shadow" "effect-blurred" "effect-background"])

(defn y-pos [{:keys [angle amplitude y-offset]}]
  (-> (js/Math.sin angle)
      (* amplitude)
      (+ y-offset)))

(defn x-pos [{:keys [x width]}]
  (mod x width))

(defn prepare [state {:keys [width height]}]
  (let [{:keys [letters
                tick
                amplitude
                angle-speed
                x-spacing
                x-speed
                rotation-multiplier
                effect-index]} @state]
    (let [y-offset (/ height 2)
          sentence-width (max width (* (+ (count letters) 1) x-spacing))]
      {:buttons [{:text "Amplitude"
                  :inc-fn #(swap! state update :amplitude inc)
                  :dec-fn #(swap! state update :amplitude dec)}
                 {:text "Periode"
                  :inc-fn #(swap! state update :angle-speed (fn [angle-speed] (max (- angle-speed 0.1) 0)))
                  :dec-fn #(swap! state update :angle-speed (fn [angle-speed] (+ angle-speed 0.1)))}
                 {:text "Avstand"
                  :inc-fn #(swap! state update :x-spacing (fn [x-spacing] (+ x-spacing 1)))
                  :dec-fn #(swap! state update :x-spacing (fn [x-spacing] (max (- x-spacing 1) 0)))}
                 {:text "Fart"
                  :inc-fn #(swap! state update :speed (fn [x] (+ x 0.01)))
                  :dec-fn #(swap! state update :speed (fn [speed] (max (- speed 0.01) 0)))}
                 {:text "Rotasjon"
                  :on-click #(swap! state update :rotation-multiplier (fn [a] (if (= 0 a)
                                                                                (:rotation-multiplier initial-state)
                                                                                0)))}
                 {:text "Effekt"
                  :on-click #(swap! state update :effect-index (fn [index] (mod (inc index) (count effects))))}

                 {:text "Tilbakestill"
                  :on-click #(reset! state initial-state)}]
       :letters (map-indexed (fn [idx letter]
                               {:text letter
                                :effect-class (nth effects effect-index)
                                :color (nth (cycle colors) idx)
                                :top (-> (y-pos {:angle (+ tick (* idx angle-speed))
                                                 :amplitude amplitude
                                                 :y-offset y-offset}))
                                :left (-> (x-pos {:x (+ (* tick x-speed) (* idx x-spacing))
                                                  :width sentence-width}))
                                :rotation (-> (js/Math.cos (+ tick (* idx angle-speed)))
                                              (* rotation-multiplier)
                                              (radians-to-degree)
                                              (str "deg"))})
                  letters)
       :width width
       :height height})))


(defn render []
  (dumdom/render (Main (prepare state {:width 600 :height 200})) (js/document.getElementById "app")))

(defn render-loop []
  (render)
  (swap! state update :tick #(+ % (:speed @state)))
  (.requestAnimationFrame js/window render-loop))

(.addEventListener js/document "DOMContentLoaded"
  (fn []
    (prn "STARTING")
    (render-loop)))

(comment
  (swap! state assoc :offset 20)
  (swap! state assoc :amplitude 30))