<p align="center">
  <a href="https://fierycod.github.io/holy-lambda" target="_blank" rel="noopener noreferrer">
    <img src="docs/media/logo.png?raw=true" alt="holy-lambda logo">
  </a>
</p>

<p align="center">
  <a href="https://github.com/FieryCod/holy-lambda-ring-adapter/actions/workflows/ci.yml"><img src="https://github.com/FieryCod/holy-lambda-ring-adapter/actions/workflows/ci.yml/badge.svg"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-green.svg"></a>
</p>

## Compatibility
  - Java Version >= 11
  - GraalVM Native Image >= 21.2.0
  - Holy Lambda >= 0.6.0

## Usage
  - **With plain [ring](https://github.com/ring-clojure/ring)**
    ```clojure
    (ns core
     (:require
      [fierycod.holy-lambda-ring-adapter.core :as hlra]
      [fierycod.holy-lambda.core :as h])
  
    (defn ring-handler
      [request]
      {:status 200
       :headers {}
       :body \"Hello World\"}
  
    (def HttpApiProxyGateway (hlra/wrap-hl-req-res-model ring-handler))
  
    (h/entrypoint [#'HttpApiProxyGateway])
    ```
  
  - **With Reitit & Muuntaja [reitit](https://github.com/metosin/reitit)***
    ```clojure
    (ns core
     (:require
      [fierycod.holy-lambda-ring-adapter.core :as hlra]
      [fierycod.holy-lambda.core :as h])
  
    (def muuntaja-ring-handler
      (ring/ring-handler
        (ring/router
          routes
          {:data {:muuntaja   instance
                  :coercion   coerction
                  :middleware middlewares}})))
  
    (def HttpApiProxyGateway (hlra/wrap-hl-req-res-model muuntaja-ring-handler))
  
    (h/entrypoint [#'HttpApiProxyGateway])
    ```

## Companies & Inviduals using Holy Lambda Ring Adapter?
  - [retailic](https://retailic.com/) 
  
## Documentation
The holy-lambda documentation is available [here](https://fierycod.github.io/holy-lambda).

## Current Version 
[![Clojars Project](https://img.shields.io/clojars/v/io.github.FieryCod/holy-lambda-ring-adapter?labelColor=283C67&color=729AD1&style=for-the-badge&logo=clojure&logoColor=fff)](https://clojars.org/io.github.FieryCod/holy-lambda-ring-adapter)

## Getting Help 
[![Get help on Slack](http://img.shields.io/badge/slack-clojurians%20%23holy--lambda-97C93C?labelColor=283C67&logo=slack&style=for-the-badge)](https://clojurians.slack.com/channels/holy-lambda)

## License
Copyright © 2021 Karol Wojcik aka Fierycod

Released under the MIT license.
