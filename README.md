# figack.core

Recreational project to learn Clojure(Script) while hopefully having some fun!

## Overview

Dungeons and Dragons a-la NetHack, using ClojureScript.

## Development

### Prerequisites

1. Install `clj` CLI tool: https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools
2. Get yourself familiar with: https://figwheel.org/

### Start the web server

```{.clj}
user=> (load "figack/server/core")
user=> (in-ns 'figack.server.core)
figack.server.core=> (start!)
```

or load the `core.clj` in CIDER and evaluate (re)`(start!)`.

### Open a game client

The above gave you the server, but you will also need a client.

#### Web client

The web client is provided using Figwheel. To get an interactive development
environment run:

    clojure -M:fig:build

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    rm -rf target/public

To create a production build run:

    rm -rf target/public
    clojure -M:fig:min

Then open the landing page URL: http://localhost:8080/index.html

#### Console client

In `hack.clj` you can also find a console client (can be useful for debugging,
and for verifying portability).

## License

Copyright © 2019 Figack Devs.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
