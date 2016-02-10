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

(defn make-dataline
  [dataline-class]
  (cast dataline-class
        (-> dataline-class (DataLine$Info. audio-format) (AudioSystem/getLine))))


(defn record-from-source
  [output-stream source-dataline]
  (do
    (.start source-dataline)
    (while (not (Thread/interrupted)) (.write source-dataline
                                              (.toByteArray output-stream)
                                              0
                                              (.size output-stream)))))

(defn play-to-target
  [output-stream target-dataline]
  (let [data (make-array Byte/TYPE (/ (.getBufferSize target-dataline) 5))]
    (while (not (Thread/interrupted)) (.write output-stream
                                              data
                                              0
                                              (.read target-dataline data 0 (count data))))))

(defn execute-neurosway
  []
  (let [source-dataline (make-dataline SourceDataLine)
        target-dataline (make-dataline TargetDataLine)
        output-stream (ByteOutputStream.)
        source-thread (Thread. (record-from-source output-stream source-dataline))
        target-thread (Thread. (play-to-target output-stream target-dataline))
        ]
    (println "Started Recording...")

    (.open source-dataline)
    (.open target-dataline)

    (.start target-thread)
    (Thread/sleep 15000)
    (-> target-dataline (.stop) (.close))

    (println "Ended Recording...\nStarted Playback...")

    (.start source-thread)
    (Thread/sleep 15000)
    (-> source-dataline (.stop) (.close))

    (println "Ended Playback...")))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (execute-neurosway))