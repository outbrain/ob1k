package com.outbrain.ob1k.example.randomcommitmessage.common;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.List;

/**
 * Created by eran on 8/20/15.
 */
public interface RandomCommitMessageService extends Service {
  ComposableFuture<List<String>> multi(int numMessages);

  ComposableFuture<String> single();
}
