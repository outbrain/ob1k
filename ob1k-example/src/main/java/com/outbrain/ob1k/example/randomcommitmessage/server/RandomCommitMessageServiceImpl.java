package com.outbrain.ob1k.example.randomcommitmessage.server;

import com.google.common.base.Function;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import com.outbrain.ob1k.example.randomcommitmessage.common.RandomCommitMessageService;
import com.outbrain.ob1k.http.HttpClient;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A service that will fetch random commit messages from http://whatthecommit.com/
 * @author Eran Harel
 */
public class RandomCommitMessageServiceImpl implements RandomCommitMessageService {

  private final HttpClient httpClient = HttpClient.createDefault();

  private final RequestBuilder requestBuilder = httpClient.get("http://whatthecommit.com/index.txt");

  @Override
  public ComposableFuture<List<String>> multi(final int numMessages) {
    final List<ComposableFuture<String>> futureMessages = new ArrayList<>(numMessages);

    for (int i = 0; i < numMessages; i++) {
      final ComposableFuture<String> messageFuture = requestBuilder.asResponse().transform(new Function<Response, String>() {
        @Override
        public String apply(final Response response) {
          try {
            return response.getResponseBody();
          } catch (final IOException e) {
            return "Failed to fetch message...";
          }
        }
      });

      futureMessages.add(messageFuture);
    }

    return ComposableFutures.all(futureMessages).materialize();
  }

  @Override
  public ComposableFuture<String> single() {
    return requestBuilder.asResponse().continueWith(new FutureResultHandler<Response, String>() {
      @Override
      public ComposableFuture<String> handle(final Try<Response> responseTry) {
        if(responseTry.isSuccess()) {
          try {
            return ComposableFutures.fromValue(responseTry.getValue().getResponseBody());
          } catch (IOException e) {
            return ComposableFutures.fromError(e);
          }
        } else {
          return ComposableFutures.fromError(responseTry.getError());
        }
      }
    });
  }
}
