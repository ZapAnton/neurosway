(ns neurosway.core
  (:gen-class)
  (:import (javax.sound.sampled AudioFormat TargetDataLine DataLine$Info AudioSystem AudioFileFormat$Type AudioFormat$Encoding AudioInputStream)
           (java.io File ByteArrayOutputStream)
           (java.awt Robot)
           (java.awt.event KeyEvent)
           (edu.emory.mathcs.jtransforms.fft DoubleFFT_1D))

  (:require [enclog.nnets :as enc-nets]
            [enclog.training :as enc-training]
            [awtbot.core :as robot])
  (:use [incanter core stats charts io datasets bayes]))

(def record-time 4000)
(def record-file (File. "sounds/record.wav"))

(def audio-format (AudioFormat.
                    AudioFormat$Encoding/PCM_SIGNED
                    44100
                    16
                    2
                    4
                    44100
                    false))

(def file-type AudioFileFormat$Type/WAVE)

(def info (DataLine$Info. TargetDataLine audio-format))


(defn read-wav-file
  [filename]
  (let [wav-file (File. filename)
        audio-input-stream (AudioSystem/getAudioInputStream wav-file)
        bytes-per-frame 8
        num-bytes (* 1024 bytes-per-frame)
        audio-bytes (make-array Byte/TYPE num-bytes)]
    (loop [result-vec []]
      (if (not= -1 (.read audio-input-stream audio-bytes))
        (recur (conj result-vec (vec audio-bytes)))
        result-vec))))

(defn hamming-transform
  [frame]
  (map-indexed
    (fn [index item]
      (* item (- 0.54 (* 0.46 (Math/cos (/ (* 2 Math/PI index) (dec (count frame))))))))
    frame))

(defn abs-fft
  [frame]
  (let [ff-transformer (DoubleFFT_1D. (count frame))
        transform-data (into-array Double/TYPE (into (into [] frame) (repeat (count frame) 0)))]
    (doto ff-transformer
      (.realForwardFull transform-data ))
    (vec transform-data)))


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
                frame)
              ))))
(defn dct
  [mel-vec]
  (for [l (range 1 (inc coef-num))]
    (reduce +
            (map-indexed
              (fn [m item]
                (* item (Math/cos (* Math/PI l (/ (+ m 0.5) coef-num)))))
              mel-vec))))

(defn process-sound-data
  [frames-vec]
  (->> frames-vec
       (map hamming-transform)
       (map abs-fft)
       (map get-mel-coefs)
       (map dct)))

(defn get-sound-data-from-file
  [filename]
  (process-sound-data (read-wav-file filename)))

;(def test1-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test1.wav"))) )
;(def test2-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test2.wav"))))
;(def test-wrong1-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test_wrong1.wav"))))
;(def test-wrong2-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test_wrong2.wav"))))
;(def test-wrong3-sound-data (future (reduce into [] (get-sound-data-from-file "sounds/test_wrong3.wav"))))
;
;(def bot (robot/create-robot))
;
;(defn alt-tab
;  []
;  (println "ALT TAB")
;  (robot/chord-keys bot KeyEvent/VK_ALT KeyEvent/VK_TAB))
;
;(defn move-mouse
;  []
;  (println "MOVING MOUSE")
;  (robot/pause bot 1000)
;  (robot/mouse-move bot {:x 4 :y 5})
;  (robot/pause bot 1000)
;  (robot/mouse-move bot {:x 400 :y 500})
;  (robot/pause bot 1000)
;  (robot/mouse-move bot {:x 200 :y 800})
;  (robot/pause bot 1000)
;  (robot/mouse-move bot {:x 40 :y 1000})
;  (robot/pause bot 1000)
;  (robot/mouse-move bot {:x 350 :y 350}))
;
;(defn train-network
;  []
;  (let [
;        input [@test1-sound-data
;               @test2-sound-data
;               @test-wrong3-sound-data
;               ]
;        ideal-output [[1.0]
;                      [1.0]
;                      [0.0]
;                      ]
;        dataset (enc-training/data :basic-dataset input ideal-output)
;        nnetwork (enc-nets/network (enc-nets/neural-pattern :feed-forward)
;                                   :activation :sigmoid
;                                   :input (count @test1-sound-data)
;                                   :output 1
;                                   :hidden [600 600])
;        trainer (enc-training/trainer :back-prop
;                                      :network nnetwork
;                                      :training-set dataset)
;        ]
;    (enc-training/train trainer 0.0001 [])
;    (let [valid-dataset (enc-training/data :basic @test-wrong2-sound-data)
;          output (.compute nnetwork valid-dataset)
;          result (vec (.getData output))
;          ]
;      (if (= 1 (Math/ceil (first result)))
;        (alt-tab)
;        (move-mouse))
;      )))

;(defn capture-sound3
;  [line]
;  (doto line
;    (.open audio-format)
;    (.start))
;  (println "Start capturing...")
;  (AudioSystem/write (AudioInputStream. line) file-type record-file))
;
;(defn record-in-file3
;  []
;  (let [line ^TargetDataLine (AudioSystem/getLine info)
;        capture-future (future (capture-sound line))]
;    (Thread/sleep record-time)
;    (future-cancel capture-future)
;    (doto line
;      (.stop)
;      (.close))
;    (println "Finished")))


(defn instant-energy
  [buffer]
  (/ (reduce + (pmap #(Math/abs (double %)) buffer)) (count buffer)))

(defn my-signum
  [num]
  (if (< num 0)
    -1
    1))

(defn zero-crosses-number
  [buffer]
  (reduce + (drop 1 (map-indexed (fn [index item]
                                   (when (not (zero? index))
                                     (let [sn item
                                           sn-1 (nth buffer (dec index))]
                                       (Math/abs (double (/ (- (my-signum sn) (my-signum sn-1)) 2.0))))))
                                 buffer))))

(def ITL (atom -1))
(def ITU (atom -1))
(def IZCT (atom -1))

(def stop-recording (atom false))
(def recording-word (atom false))

(def recorded-data (atom []))
(def words (atom []))

(defn check-frame
  [buffer]
  (let [buffer-energy (instant-energy buffer)
        buffer-zeroes (zero-crosses-number buffer)]
    (println (str "CHECKING: \nITL = " @ITL "\nBuffer Energy = " buffer-energy))
    (cond
      (and (> buffer-energy @ITL) (< buffer-zeroes @IZCT)) (reset! recorded-data (vec buffer))

      (and (> buffer-energy @ITL) (> buffer-zeroes @IZCT)) (do
                                                             (reset! recording-word true)
                                                             (swap! recorded-data into buffer))

      (and (< buffer-energy @ITL) (< buffer-zeroes @IZCT) (true? @recording-word)) (do
                                                                                     (reset! recording-word false)
                                                                                     (swap! recorded-data into buffer)
                                                                                     (swap! words conj @recorded-data)
                                                                                     (reset! recorded-data [])))

    (if (> buffer-energy @ITL)
      (do
        (println "INTO WORD!")
        (when (not @recording-word)
          (reset! recording-word true))
        (swap! recorded-data into buffer)))
    (println)))

(defn process-frame
  [buffer]
  (if (= -1 @ITL)
    (do
      (reset! ITL (instant-energy buffer))
      (reset! ITU (* @ITL 2.5))
      (reset! IZCT (zero-crosses-number buffer)))
    (check-frame buffer))
  )

(defn capture-sound
  []
  (let [line ^TargetDataLine (AudioSystem/getLine info)
        buffer-size (* (.getSampleRate audio-format) (.getFrameSize audio-format))
        out (ByteArrayOutputStream.)
        buffer (make-array Byte/TYPE buffer-size)]
    (doto line
      (.open audio-format)
      (.start))
    (println "STARTED LINE!")
    (while (not @stop-recording)
      (let [bytes-read (.read line buffer 0 buffer-size)]
        (when (> bytes-read 0)
          (.write out buffer 0 bytes-read)
          (future (process-frame buffer)))))
    (doto line
      (.stop)
      (.close))
    (.close out)
    (println "CLOSED LINE!")))

(defn watch
  [key watched old new]
  (println (str "OLD: " old " NEW: " new "\n")))

(add-watch ITL :key watch)

(defn split-vec
  [vec]
  (loop [start-index 0
         result []]
    (let [end-index (+ start-index 3)]
      (if (> end-index (count vec))
        (conj result (subvec vec start-index (count vec)))
        (recur (+ start-index 1) (conj result (subvec vec start-index end-index)))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (if (not (AudioSystem/isLineSupported info))
    (println "Line Is Not Supported. The program will end")
    (do (println "The line is supported. Sound capture starts now.")
        (reset! stop-recording false)
        (future (capture-sound))
        (Thread/sleep 10000)
        (println "STOPPING THE LINE!")
        (reset! stop-recording true)
        (Thread/sleep 1000)
        (reset! ITL -1)
        (reset! ITU -1)
        (reset! IZCT -1)
        (println "\n\nWORDS!!!!: " (count @words))
        (reset! recorded-data [])
        (reset! words []))))