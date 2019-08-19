# java-istio

This project demonstrates Istio is broken on Java 9 and beyond.

## WHAT??

### Given

* A **plain vanilla** istio cluster with mTLS disabled, traffic policy is `ALLOW_ANY`. No other configuration.
* `echo-server` is a plain vanilla http echo server running on tomcat, created from https://start.spring.io
* `echo-client` is a plain vanilla client that calls the echo server, using regular built-in Java HTTP client
* They have ports declared as `http` as per [Istio requirements](https://istio.io/docs/setup/kubernetes/additional-setup/requirements/)

### Steps to Reproduce

* `kubectl apply -f k8s.yaml`
* `kubectl get pods --all-namespaces`: Wait for `echo-server` and `echo-client` to be up and running
* Open echo server at `http://<cluster>:30181/` – it works fine
* Now open echo client at `http://<cluster>:30182/`
* **Expected:** Echo client talks to echo server
* **Actual**: Istio blocks the request with `HTTP 403`

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
* This might be tolerable if the port was declared as `http2`. But in this case, the port is clearly labelled as `http` and even then Istio breaks.

**TL;DR:** – Any plain java HTTP call within cluster using standard, built-in HTTP clients and specs is broken on Istio.

Forget about advanced stuff like Traffic routing and so on. Just point-to-point HTTP call does not work.
