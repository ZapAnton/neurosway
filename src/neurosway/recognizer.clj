(ns neurosway.recognizer
  (:import (java.awt.event KeyEvent)
           (edu.emory.mathcs.jtransforms.fft DoubleFFT_1D))

  (:require [enclog.nnets :as enc-nets]
            [enclog.training :as enc-training]
            [awtbot.core :as robot])

  (:use [neurosway.sound-utils]))


(defn hamming-transform
  [frame]
  (map-indexed
    (fn [index item]
      (* item (- 0.538 (* 0.462 (Math/cos (/ (* 2 Math/PI index) (dec (count frame))))))))
    frame))

(defn my-fft
  [frame]
  (let [ff-transformer (DoubleFFT_1D. (count frame))
        transform-data (into-array Double/TYPE (into (into [] frame) (repeat (count frame) 0)))]
    (doto ff-transformer
      (.realForwardFull transform-data ))
    (subvec (vec transform-data) 0 (count frame))))

(def frame-size (* 8 1024))
(def mel-window [300, 517.33, 781.90, 1103.97, 1496.04, 1973.32, 2554.33, 3261.62, 4122.63, 5170.76, 6446.70, 8000])
(def f (map #(Math/floor (/ (* % (inc frame-size)) 44100)) mel-window))
(def coef-num 10)

(defn mel-filter
  [m k]
  (cond
    (< k (nth f (dec m))) 0
    (and (<= (nth f (dec m)) k) (<= k (nth f m))) (/ (- k (nth f (dec m))) (- (nth f m) (nth f (dec m))))
    (and (<= (nth f m) k) (<= k (nth f (inc m)))) (/ (- (nth f (inc m)) k) (- (nth f (inc m)) (nth f m)))
    (> k (nth f (inc m))) 0
    ))

(defn get-mel-coefs
  [frame]
  (for
    [x (range 1 (inc coef-num))]
    (Math/log
      (reduce +
              (map-indexed
                (fn [index item]
                  (* item item
                     (mel-filter x index)))
                frame)))))
(defn dct
  [mel-vec]
  (for [l (range 1 (inc coef-num))]
    (reduce +
            (map-indexed
              (fn [m item]
                (* item (Math/cos (* Math/PI l (/ (+ m 0.5) coef-num)))))
              mel-vec))))

(defn process-sound-data
  [fragmented-word]
  (->> fragmented-word
       (map hamming-transform)
       (map my-fft)
       (map get-mel-coefs)
       (map dct)))

(defn get-sound-data-from-file
  [filename]
  (process-sound-data (read-wav-file filename)))

(def test1-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test1.wav"))) )
(def test2-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test2.wav"))))
(def test-wrong1-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test_wrong1.wav"))))
(def test-wrong2-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test_wrong2.wav"))))
(def test-wrong3-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test_wrong3.wav"))))

(def bot (robot/create-robot))

(defn alt-tab
  []
  (println "ALT TAB")
  (robot/chord-keys bot KeyEvent/VK_ALT KeyEvent/VK_TAB))

(defn move-mouse
  []
  (println "MOVING MOUSE")
  (robot/pause bot 1000)
  (robot/mouse-move bot {:x 4 :y 5})
  (robot/pause bot 1000)
  (robot/mouse-move bot {:x 400 :y 500})
  (robot/pause bot 1000)
  (robot/mouse-move bot {:x 200 :y 800})
  (robot/pause bot 1000)
  (robot/mouse-move bot {:x 40 :y 1000})
  (robot/pause bot 1000)
  (robot/mouse-move bot {:x 350 :y 350}))

(def network-recognizer (enc-nets/network (enc-nets/neural-pattern :feed-forward)
                                          :activation :sigmoid
                                          :input 250
                                          :output 1
                                          :hidden [600 600]))

(defn train-network
  "Обучение нейронной сети"
  []
  (let [
        input [@test1-sound-data
               @test2-sound-data
               @test-wrong3-sound-data
               ]
        ideal-output [[1.0]
                      [1.0]
                      [0.0]
                      ]
        dataset (enc-training/data :basic-dataset input ideal-output)
        nnetwork (enc-nets/network (enc-nets/neural-pattern :feed-forward)
                                   :activation :sigmoid
                                   :input (count @test1-sound-data)
                                   :output 1
                                   :hidden [600 600])
        trainer (enc-training/trainer :back-prop
                                      :network nnetwork
                                      :training-set dataset)
        ]
    (enc-training/train trainer 0.0001 [])
    (let [valid-dataset (enc-training/data :basic @test-wrong2-sound-data)
          output (.compute nnetwork valid-dataset)
          result (vec (.getData output))
          ]
      (if (= 1 (Math/ceil (first result)))
        (alt-tab)
        (move-mouse)))))

(defn process-command
  "Получение параметров входного слова"
  [word]
  (let [N (/ (count word) 25)
        fragmented-word (partition N word)
        word-parameters (process-sound-data fragmented-word)
        param-vec (vec (flatten word-parameters))]
    ))