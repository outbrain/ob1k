package com.outbrain.ob1k.example.randomcommitmessage.client;

import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.example.randomcommitmessage.common.RandomCommitMessageService;
import rx.Observable;

/**
 * @author Eran Harel
 */
public class RandomCommitMessageClient {

  public static final RandomCommitMessageService SERVICE = new RandomCommitMessageClient().randomCommitMessageService;
  public static final String SERVER_HOST = "http://localhost:8080";

  private final RandomCommitMessageService randomCommitMessageService;

  private RandomCommitMessageClient() {
    randomCommitMessageService = new ClientBuilder<>(RandomCommitMessageService.class)
      .setReadTimeout(1000)
      .setRequestTimeout(800)
      .setConnectionTimeout(400)
      .setTargetProvider(new SimpleTargetProvider(SERVER_HOST + "/rcm/whatthecommit"))
      .build();
  }

  public static void main(final String[] args) throws Exception {
    System.out.println("Your message Sir: " + SERVICE.single().get());
    System.out.println("Your messages Sir: " + SERVICE.multi(2).get());

    final Observable<String> messagesStream = SERVICE.stream(10);
    messagesStream.toBlocking().forEach(message -> System.out.println("Your message from stream: " + message));

    System.out.println("Done.");

    Clients.close(SERVICE);
  }
}
