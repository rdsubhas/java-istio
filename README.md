# java-istio

This project demonstrates Istio is broken on Java 9 and beyond.

* On Java 9 and 10 – it's broken with any library that tries to do HTTP/2.
* On Java 11 and above – it's broken at the language level for every HTTP request.

## WHAT??

### Given

* A **plain vanilla** istio cluster with mTLS disabled, traffic policy is `ALLOW_ANY`. No other configuration.
* `echo-server` is a plain vanilla http echo server running on tomcat, created from https://start.spring.io
* `echo-client` is a plain vanilla client that calls the echo server, using regular built-in Java HTTP client
* They have ports declared as `http` as per [Istio requirements](https://istio.io/docs/setup/kubernetes/additional-setup/requirements/)
* `echo-server` has istio sidecar injected. `echo-client` may or may not have istio injected, result is same.

### Steps to Reproduce

* `kubectl apply -f k8s.yaml`
* `kubectl get pods --all-namespaces`: Wait for `echo-server` and `echo-client` to be up and running
* Open echo server at `http://<cluster>:30181/` – it works fine
* Now open echo client at `http://<cluster>:30182/`
* **Expected:** Echo client talks to echo server
* **Actual**: Istio blocks the request with `HTTP 403`

![HTTP 403](/output.png?raw=true "HTTP 403")

## WHY??

* HTTP/2 upgrade spec: https://httpwg.org/specs/rfc7540.html#discover-http
  * Generally, clients (web browsers, Java, etc) don't know beforehand whether the server they are calling supports HTTP/1 or HTTP/2
  * As outlined in the spec, they will send `Upgrade: h2c` and the other headers as an indicator
  * The server can respond by switching protocols, again as per spec
  * But if not, the spec clearly states, the server can process the request as if it was normal HTTP/1.1
  
    > A server that does not support HTTP/2 can respond to the request as though the Upgrade header field were absent.

None of this is custom. It's as per standard. That's how Google Chrome can talk to your HTTP/1.1 servers today. Spring boot supports this mode from Java 9 onwards. Java 11 and onwards it's the default behavior.

* Istio needs ports to be prefixed with protocol: https://istio.io/docs/setup/kubernetes/additional-setup/requirements/
* If it's a HTTP service, of course, we prefix as `http`
* If it gets a HTTP/2 request, as per spec, **it should ignore the `Upgrade`** as per the spec
* But istio fails the request outright with `403 Forbidden`

This might be tolerable if the port was declared as `http2`. But in this case, the port is clearly labelled as `http` and even then Istio breaks.

This is purely on the **incoming** istio sidecar. Not on egress. That's what makes this bug even more annoying. For egress ports, there are LDS limits and so on. But on the incoming side, there is simply no ambiguity on the container port's name. But even if there **was** ambiguity, the spec already clearly addresses it and says to ignore the optional headers and do normal HTTP.

## TL;DR

Any plain java HTTP call within cluster using standard, built-in HTTP clients and specs is broken on Istio. Forget about advanced stuff like Traffic routing and so on. Just point-to-point call will not work.

## HOW CAN I PREVENT THIS??

1. Mark http ports as `tcp`
2. (Or) Disable istio on the target namespace.
