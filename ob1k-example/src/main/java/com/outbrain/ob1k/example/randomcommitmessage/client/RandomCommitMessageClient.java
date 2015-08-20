package com.outbrain.ob1k.example.randomcommitmessage.client;

import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.example.randomcommitmessage.common.RandomCommitMessageService;

import static com.outbrain.ob1k.HttpRequestMethodType.GET;

/**
 * Created by eran on 8/20/15.
 */
public class RandomCommitMessageClient {

  public static final RandomCommitMessageService SERVICE = new RandomCommitMessageClient().randomCommitMessageService;

  private final RandomCommitMessageService randomCommitMessageService;

  private RandomCommitMessageClient() {
    randomCommitMessageService = new ClientBuilder<>(RandomCommitMessageService.class)
      .setReadTimeout(1000)
      .setRequestTimeout(800)
      .setConnectionTimeout(400)
      .setTargetProvider(new SimpleTargetProvider("http://localhost:8080/rcm/whatthecommit"))
      .bindEndpoint("single", GET, "/single")
      .bindEndpoint("multi", GET, "/multi/{numMessages}")
      .build();
  }

  public static void main(final String[] args) throws Exception {
    System.out.println("Your message Sir: " + SERVICE.single().get());
    System.out.println("Your messages Sir:\n" + SERVICE.multi(2).get());

    Clients.close(SERVICE);
  }
}
