(defproject neurosway "0.2.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.encog/encog-core "3.2.0"]
                 [enclog "0.6.5"]
                 [net.sourceforge.jtransforms/jtransforms "2.4.0"]
                 [awtbot "1.0.0"]
                 [incanter "1.5.7"]]
  :main ^:skip-aot neurosway.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
