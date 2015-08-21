package com.outbrain.ob1k.example.hello.client;

import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.example.hello.api.HelloService;
import com.outbrain.ob1k.example.hello.server.HelloServer;
import com.outbrain.ob1k.http.common.ContentType;

/**
 * Basic example of how using Ob1k's RPC client,
 * using our HelloService implementation
 *
 * @author marenzon
 */
public class HelloServiceClient {

  private static final int CLIENT_REQUEST_TIMEOUT_MS = 1000;

  public static void main(final String[] args) throws Exception {

    // Initializing new server for the client
    final HelloServer helloServer = new HelloServer();
    helloServer.start(HelloServer.PORT);

    System.out.println("Started new server on port " + HelloServer.PORT);

    // Building the target url, where the service we're gonna use located
    final String serviceUri = HelloServer.CTX_PATH + HelloServer.HELLO_SERVICE_PATH;
    final String target = "http://localhost:" + HelloServer.PORT + serviceUri;

    // Creating new RPC client, using our service api interface
    final HelloService helloService = new ClientBuilder<>(HelloService.class).
      setProtocol(ContentType.JSON).
      setRequestTimeout(CLIENT_REQUEST_TIMEOUT_MS).
      setTargetProvider(new SimpleTargetProvider(target)).
      build();

    // Calling service endpoint asynchronous, receiving a ComposableFuture back on each action
    System.out.println("Calling helloWorld endpoint");
    helloService.helloWorld().consume(result -> System.out.println("[" + serviceUri + "/helloWorld]: " + result));

    System.out.println("Calling helloUser endpoint");
    helloService.helloUser("Moshe").consume(result -> System.out.println("[" + serviceUri + "/helloUser]: " + result));

    System.out.println("Calling helloUserStream endpoint");
    helloService.helloUserStream("Haim", 10).toBlocking().forEach(result -> System.out.println("[stream event]: " + result));

    System.out.println("Closing the client, and the server");
    // Closing down the client
    Clients.close(helloService);
    // Shutting down the server
    helloServer.stop();
  }
}