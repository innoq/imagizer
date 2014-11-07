imagizer
========

This is a simple demo project built with Clojure for a talk about Clojure in practice and covers the basics of a simple web application.

## Setup

### Leiningen

For this project you need [Leiningen](http://leiningen.org/) and of course a current JDK.

### ImageMagick

Furthermore, you should install [ImageMagick](http://www.imagemagick.org/), e.g., if you're on Mac OS and using homebrew:

    brew install imagemagick

### Start

    lein cljsbuild once
    lein ragtime migrate
    lein ring server

This will start a webserver which is listening on port 3000 and open a browser (if you don't want a browser window to open automatically try `server-headless` instead of `server` and navigate to [http://localhost:3000/](http://localhost:3000/)).

## Useful links

* [Slides 'Clojure in Practice'](https://www.innoq.com/de/talks/2014/11/clojure-in-der-praxis-wjax-2014/)
* [Learning Clojure in the browser](http://www.4clojure.com/problems)
* [Documentation for Clojure](http://clojure.org/documentation) / [CheatSheet](http://clojure.org/cheatsheet)
* [Documentation for Leiningen](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md)

## Dependencies

Here is a list of the dependencies and their documentation:

* [Ring](https://github.com/ring-clojure/ring)
* [Compojure](https://github.com/weavejester/compojure)
* [Hiccup](https://github.com/weavejester/hiccup)
* [clj-http](https://github.com/dakrone/clj-http)
* [Hickory](https://github.com/davidsantiago/hickory)
* [Yesql](https://github.com/krisajenkins/yesql)
* [Ragtime](https://github.com/weavejester/ragtime)
* [java.jdbc](https://github.com/clojure/java.jdbc/)
* [h2](http://www.h2database.com/html/main.html)
* [im4java](http://im4java.sourceforge.net/)
* [ClojureScript](https://github.com/clojure/clojurescript)
* [dommy](https://github.com/Prismatic/dommy)
* [cljs-http](https://github.com/r0man/cljs-http)
* [core.async](https://github.com/clojure/core.async)

## Author information and license

Copyright 2014 innoQ Deutschland GmbH. Published under the Apache 2.0 license.