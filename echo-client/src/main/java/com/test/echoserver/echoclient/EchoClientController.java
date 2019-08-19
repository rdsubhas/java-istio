package com.test.echoserver.echoclient;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class EchoClientController {

    @RequestMapping(produces = "application/json", method = RequestMethod.GET, value = "/")
    public ResponseEntity<Object> index() throws Exception {

        HttpRequest echoRequest = HttpRequest.newBuilder()
                .uri(new URI(System.getenv("ECHO_SERVER_URL")))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> echoResponse = HttpClient.newHttpClient()
                .send(echoRequest, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> response = new HashMap<>();
        response.putAll(echoResponse.headers().map());
        response.put("body", echoResponse.body());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
