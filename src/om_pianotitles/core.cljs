(ns om-pianotitles.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]])
  (:import [goog.ui IdGenerator]))

(enable-console-print!)

(defn generate-cols [n] 
  (into [] 
        (map-indexed 
          (fn [id] 
            (let [value (if (= id n) true false)]
              {:value value :selected false}))
          (range 4))))

(defn generate-rows []
  (mapv (fn [_] (let [n (rand-int 4)] (generate-cols n)))
        (range 10)))

(defn tile [data owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [tile-selected]}]
      (let [className (if (:value data) "tile black" "white tile")] 
        (html [:li {:key data 
                    :className className
                    :onClick (fn [_] (put! tile-selected @data))}])))))

(defn tiles-rows [data owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [tile-selected]}]
      (html [:li {:key data 
                  :className "tiles-list"}
             [:ul (om/build-all tile data {:init-state 
                                           {:tile-selected tile-selected}})]]))))

(defn change-scroll-position [owner] 
  (let [node (om/get-node owner)]
    (let [height (- (.-scrollTop node) 130)]
      (set! (.-scrollTop node) height))))

(defn piano [data owner]
  (reify
    om/IInitState
    (init-state [_] 
      {:tile-selected (chan)})
    om/IWillMount
    (will-mount [this]
      (let [tile-selected (om/get-state owner :tile-selected)]
        (go-loop [] (let [tile (<! tile-selected)]
                      (if (:value tile)
                        (change-scroll-position owner)
                        (om/transact! data :cursor inc))
                      (recur)))))
    om/IDidMount
    (did-mount [this]
      (let [node (om/get-node owner)]
        (let [height (. node -scrollHeight)]
          (set! (. node -scrollTop) height))))

    om/IRenderState
    (render-state [this {:keys [tile-selected]}] 
      (html [:ul {:className "piano"} 
             (om/build-all tiles-rows (data :list) {:init-state {:tile-selected tile-selected}})]))))

(defonce app-state (atom {:list (generate-rows) :cursor 0}))

(om/root piano app-state {:target (. js/document (getElementById "app"))})

