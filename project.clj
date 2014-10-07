(defproject imagizer "0.1.0-SNAPSHOT"
  :description "yet another clojure demo app"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.3.1"]
                 [compojure "1.1.9"]
                 [hiccup "1.0.5"]
                 [clj-http "1.0.0"]
                 [hickory "0.5.4"]
                 [org.im4java/im4java "1.4.0"]
                 [org.clojure/clojurescript "0.0-2356"]]
  :plugins [[lein-ring "0.8.11"]
            [lein-cljsbuild "1.0.3"]]
  :ring {:handler imagizer.core/webapp}
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/imagizer.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
