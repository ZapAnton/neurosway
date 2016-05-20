(ns neurosway.core
  (:gen-class)
  (:import (javax.sound.sampled  AudioSystem))

  (:use [incanter core stats charts io datasets bayes]
        [neurosway.sound-utils]
        [neurosway.input-signal-processor]))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (if (not (AudioSystem/isLineSupported info))
    (println "Line Is Not Supported. The program will end")
    (do
      (println "The line is supported. Sound capture starts now.")
      (reset! stop-recording false)
      (future (capture-sound))

      (Thread/sleep 10000)
      (println "STOPPING THE LINE!")
      (reset! stop-recording true)

      (Thread/sleep 1000)
      (println "\n\nWORDS!!!!: " (count @words))
      (reset-sound-system!))))