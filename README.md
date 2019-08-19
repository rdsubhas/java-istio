# java-istio

This project demonstrates Istio is broken on Java 9 and beyond.

## WHAT??

Given:

* A **plain vanilla** istio cluster.
* mTLS disabled. Default traffic policy is `ALLOW_ANY`. No other configuration.
* `echo-server` is a plain echo server on java 9.
* `echo-client` simply calls echo-server and prints the response

Then:

* Open `http://<cluster>/echo-server` – It prints what you requested
* Open `http://<cluster>/echo-client` - **It fails with `HTTP 403`**

## WHY??

* Java 9 supports HTTP/2
  * It conforms to HTTP/2 spec: https://httpwg.org/specs/rfc7540.html#discover-http used worldwide
  * Generally, clients (web browsers, Java, etc) don't know beforehand whether the server they are calling supports HTTP/1 or HTTP/2
  * As outlined in the spec, they will send `Upgrade: h2c` and the other headers as an indicator
  * The server can respond by switching protocols, again as per spec
  * But if not, the spec clearly states, the server can process the request as if it was normal HTTP/1.1
    
    > A server that does not support HTTP/2 can respond to the request as though the Upgrade header field were absent.

None of this is custom. It's as per standard. That's how Google Chrome can talk to your HTTP/1.1 servers today.

* Istio needs ports to be prefixed with protocol: https://istio.io/docs/setup/kubernetes/additional-setup/requirements/
* If it's a HTTP service, of course, we prefix as `http`
* If it gets a HTTP/2 request, as per spec, **it should ignore the `Upgrade`**
* But istio fails the request outright with `403 Forbidden`

Basically Istio breaks `http` protocol, because it had an optional, ignorable header as per spec.

## 