(defproject imagizer "0.1.0-SNAPSHOT"
  :description "clojure demo app"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.3.1"]
                 [compojure "1.1.9"]
                 [hiccup "1.0.5"]
                 [clj-http "1.0.0"]
                 [hickory "0.5.4"]
                 [org.im4java/im4java "1.4.0"]
                 [org.clojure/clojurescript "0.0-2356"]
                 [prismatic/dommy "0.1.3"]
                 [cljs-http "0.1.16"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [yesql "0.4.0"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [com.h2database/h2 "1.4.181"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [ragtime "0.3.7"]]
  :plugins [[lein-ring "0.8.12"]
            [lein-cljsbuild "1.0.3"]
            [ragtime/ragtime.lein "0.3.7"]]
  :ring {:handler imagizer.core/webapp}
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/js/imagizer.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:h2:./db/data"}
  :profiles {:uberjar {:aot :all}})
