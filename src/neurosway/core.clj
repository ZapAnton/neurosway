(ns neurosway.core
  (:gen-class)
  (:import [javax.sound.sampled AudioFormat AudioFormat$Encoding DataLine$Info SourceDataLine AudioSystem TargetDataLine]
           (com.sun.xml.internal.messaging.saaj.util ByteOutputStream)))

(def audio-format (AudioFormat.
              AudioFormat$Encoding/PCM_SIGNED
              44100
              16
              2
              4
              44100
              false))

(def source-info (DataLine$Info.
                  SourceDataLine
                  audio-format))

(def source-dataline (cast SourceDataLine (AudioSystem/getLine source-info)))

(.open source-dataline)

(def target-info (DataLine$Info.
                   TargetDataLine
                   audio-format))

(def target-dataline (cast TargetDataLine (AudioSystem/getLine target-info)))

(.open target-dataline)

(def output-stream (ByteOutputStream.))

(def source-delay (delay (do
                            (.start source-dataline)
                            (while (not (Thread/interrupted)) (.write source-dataline
                                                (.toByteArray output-stream)
                                                0
                                                (.size output-stream))))))

(def target-delay (delay (let [data (make-array Byte/TYPE (/ (.getBufferSize target-dataline) 5))]
                            (while (not (Thread/interrupted)) (.write output-stream
                                                                      data
                                                                      0
                                                                      (.read target-dataline data 0 (count data)))))))

(defn execute-neurosway
  []
  (do
    (println "Started Recording...")
    (force target-delay)
    (Thread/sleep 15000)
    (-> target-dataline (.stop) (.close))
    (println "Ended Recording...\nStarted Playback...")
    (force source-delay)
    (Thread/sleep 15000)
    (-> source-dataline (.stop) (.close))
    (println "Ended Playback...")))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (execute-neurosway))