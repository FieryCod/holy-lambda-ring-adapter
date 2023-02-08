<p align="center">
  <a href="https://fierycod.github.io/holy-lambda" target="_blank" rel="noopener noreferrer">
    <img src="docs/media/logo.png?raw=true" alt="holy-lambda logo">
  </a>
</p>

<p align="center">
  <a href="https://github.com/FieryCod/holy-lambda-ring-adapter/actions/workflows/ci.yml"><img src="https://github.com/FieryCod/holy-lambda-ring-adapter/actions/workflows/ci.yml/badge.svg"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-green.svg"></a>
</p>

## Rationale
I wanted to create a library that allows to use known Clojure tools to develop API's on AWS Lambda.

**A library that**

- prevents AWS to vendor lock you with Lambda,
- allows for fast feedback loop while developing API locally,
- implements a full Ring spec,
- supports serving resources from AWS Lambda,
- is fast, so that cold starts are minimal

This is why holy-lambda-ring-adapter was released. An adapter is a part of holy-lambda project and is already used in production.

## Compatibility
  - AWS ApiGateway Lambda Integration
    - [HttpApi](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop.html#http-api-examples)
    - [RestApi](https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-rest-api.html) 
  - Java Version >= 11 or Babashka >= 0.8.2
  - GraalVM Native Image >= 21.2.0
  - Holy Lambda >= 0.6.0 [all backends: [native](https://fierycod.github.io/holy-lambda/#/native-backend-tutorial), [babashka](https://fierycod.github.io/holy-lambda/#/babashka-backend-tutorial), [clojure](https://fierycod.github.io/holy-lambda/#/clojure-backend-tutorial)

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
       :body "Hello World"})
  
    (def HttpApiProxyGateway (hlra/ring<->hl-middleware ring-handler))
  
    (h/entrypoint [#'HttpApiProxyGateway])
    ```
  
  - **With Reitit & Muuntaja [reitit](https://github.com/metosin/reitit)**
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
  
    (def HttpApiProxyGateway (hlra/ring<->hl-middleware muuntaja-ring-handler))
  
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
