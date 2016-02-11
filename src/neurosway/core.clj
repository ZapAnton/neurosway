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


(defn read-from-target
  [output-stream target-dataline data]
  (.start target-dataline)
  (while (not (Thread/interrupted)) (.write output-stream
                                            data
                                            0
                                            (.read target-dataline data 0 (count data)))))

(defn play-to-source
  [output-stream source-dataline]
  (.start source-dataline)
  (while (not (Thread/interrupted)) (.write source-dataline
                                            (.toByteArray output-stream)
                                            0
                                            (.size output-stream))))


(defn check-dataline
  [dataline]
  (println "\nDataline: " (class dataline) "\nIs Active: " (.isActive dataline)  "\nIs Open: " (.isOpen dataline)  "\nIs Running: " (.isRunning dataline) "\n" ))

(defn execute-neurosway
  []
  (let [
        source-dataline (make-dataline SourceDataLine)
        target-dataline (make-dataline TargetDataLine)
        output-stream (ByteOutputStream.)
        record-time 5000
        ]
    (println "Started Recording...")

    (.open source-dataline)
    (.open target-dataline)

    (deref (future (read-from-target output-stream target-dataline (make-array Byte/TYPE (/ (.getBufferSize target-dataline) 5)))) record-time :ok)
    (.stop target-dataline)
    (.close target-dataline)

    (println "Recorded: " (get (.toByteArray output-stream) 0))
    (println "Ended Recording...\nStarted Playback...")

    (deref (future (play-to-source output-stream source-dataline)) record-time :ok)

    (.stop source-dataline)
    (.close source-dataline)
    (println "Ended Playback...")
    ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (execute-neurosway))























