imagizer
========

## Setup

First, you should install [ImageMagick](http://www.imagemagick.org/), e.g. if you're on Mac OS and using homebrew:

    brew install imagemagick

Then:

    lein cljsbuild once
    lein ragtime migrate
    lein ring server

This will start a webserver which is listening on port 3000 and open a browser (if you don't want a browser window to open automatically try server-headless).

## Author information and license

Copyright 2014 innoQ Deutschland GmbH. Published under the Apache 2.0 license.