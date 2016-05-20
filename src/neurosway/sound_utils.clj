(ns neurosway.sound-utils
  (:import (javax.sound.sampled AudioFormat$Encoding AudioFormat AudioFileFormat$Type DataLine$Info TargetDataLine AudioSystem)
           (java.io File)))

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


(defn split-vec
  [vec]
  (loop [start-index 0
         result []]
    (let [end-index (+ start-index 3)]
      (if (> end-index (count vec))
        (conj result (subvec vec start-index (count vec)))
        (recur (+ start-index 1) (conj result (subvec vec start-index end-index)))))))