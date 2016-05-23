(ns neurosway.input-signal-processor
  (:import (java.io ByteArrayOutputStream)
           (javax.sound.sampled AudioSystem TargetDataLine))
  (:use [neurosway.sound-utils]
        [neurosway.recognizer]))

(def ITL (atom -1))
(def ITU (atom -1))
(def IZCT (atom -1))

(def stop-recording (atom false))
(def recording-word (atom false))

(def recorded-data (atom []))
(def words (atom []))

(defn watch
  [key watched old new]
  (println (str "OLD: " old " NEW: " new "\n")))

(add-watch ITL :key watch)

(defn- instant-energy
  [buffer]
  (/ (reduce + (pmap #(Math/abs (double %)) buffer)) (count buffer)))

(defn- my-signum
  [num]
  (if (< num 0)
    -1
    1))

(defn- zero-crosses-number
  [buffer]
  (reduce + (drop 1 (map-indexed (fn [index item]
                                   (when (not (zero? index))
                                     (let [sn item
                                           sn-1 (nth buffer (dec index))]
                                       (Math/abs (double (/ (- (my-signum sn) (my-signum sn-1)) 2.0))))))
                                 buffer))))


(defn- check-frame
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
                                                                                     (future (println "FOUND WORD!!!!"))
                                                                                     (process-command @recorded-data)
                                                                                     (reset! recorded-data [])))
    (println)))

(defn- process-frame
  [buffer]
  (if (= -1 @ITL)
    (do
      (reset! ITL (instant-energy buffer))
      (reset! ITU (* @ITL 2.5))
      (reset! IZCT (zero-crosses-number buffer)))
    (check-frame buffer)))

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

(defn reset-sound-system!
  []
  (reset! ITL -1)
  (reset! ITU -1)
  (reset! IZCT -1)
  (reset! recorded-data [])
  (reset! words []))