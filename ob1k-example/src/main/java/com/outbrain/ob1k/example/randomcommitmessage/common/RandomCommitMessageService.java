package com.outbrain.ob1k.example.randomcommitmessage.common;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

import java.util.List;

/**
 * RandomCommitMessageService Interface
 * Implemented by the server, used by the client for the RPC requests.
 *
 * @author Eran Harel
 */
public interface RandomCommitMessageService extends Service {
  ComposableFuture<List<String>> multi(int numMessages);

  ComposableFuture<String> single();

  Observable<String> stream(int numMessages);
}
