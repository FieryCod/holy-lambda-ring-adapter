# CHANGELOG

## 0.1.2
- Support AWS ApiGateway RestApi 
- Fix asynchronous error reporting
- Throw meaningful error message on incorrect event shape instead of NPE 

## 0.1.1
- Deprecate `wrap-hl-req-res-model`. Use `ring<->hl-middleware` instead.

## 0.1.0
- Add support for async non-async handlers
- Support all Ring types
- Support all holy-lambda backends
- Support HttpAPI as Lambda Integration

