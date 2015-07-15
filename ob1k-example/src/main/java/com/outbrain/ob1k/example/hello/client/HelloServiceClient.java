package com.outbrain.ob1k.example.hello.client;

import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.example.hello.server.HelloServer;
import rx.Observable;

/**
 * @author marenzon
 */
public class HelloServiceClient {

  public static void main(final String[] args) throws Exception {

    final HelloServer helloServer = new HelloServer();
    helloServer.start(HelloServer.PORT);

    final String target = "http://localhost:" + HelloServer.PORT + HelloServer.CTX_PATH + HelloServer.HELLO_SERVICE_PATH;
    final IHelloService helloService = new ClientBuilder<>(IHelloService.class).
            setProtocol(ContentType.JSON).
            setRequestTimeout(-1).
            setTargetProvider(new SimpleTargetProvider(target)).
            build();

    final Observable<String> helloMosheStream = helloService.helloUserStream("Moshe", 5);
    helloMosheStream.toBlocking().forEach(System.out::println);

    final ComposableFuture<String> helloWorld = helloService.helloWorld();
    final ComposableFuture<String> helloMoshe = helloService.helloUser("Moshe");

    final ComposableFuture<String> resultsMergeFuture = ComposableFutures.combine(helloWorld, helloMoshe,
            (String left, String right) -> left + "\n" + right + "\n" + "And welcome to ob1k.");

    final String welcomeMessage = resultsMergeFuture.get();
    System.out.println(welcomeMessage);

    Clients.close(helloService);
    helloServer.stop();
  }
}