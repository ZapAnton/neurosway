(ns clojure-learn.core
  (:gen-class)
  (:import (javax.sound.sampled AudioFormat TargetDataLine DataLine$Info AudioSystem AudioFileFormat$Type AudioFormat$Encoding AudioInputStream)
           (java.io File)))

(def record-time 6000)
(def record-file (File. "record.wav"))

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

(defn capture-sound
  [line]
  (doto line
    (.open audio-format)
    (.start))
  (println "Start capturing...")
  (AudioSystem/write (AudioInputStream. line) file-type record-file))

(defn record-in-file
  []
  (let [line ^TargetDataLine (AudioSystem/getLine info)
        capture-future (future (capture-sound line))]
    (Thread/sleep record-time)
    (future-cancel capture-future)
    (doto line
      (.stop)
      (.close))
    (println "Finished")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (if (not (AudioSystem/isLineSupported info))
    (println "Line Is Not Supported. The program will end")
    (do (println "The line is supported. Sound capture starts now.")
        (record-in-file))))
