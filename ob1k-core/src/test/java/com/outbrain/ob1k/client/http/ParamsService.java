package com.outbrain.ob1k.client.http;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

public class ParamsService implements Service {
  public ComposableFuture<Map<String, String>> handle(final HttpRequest request) {
    final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
    final Map<String, List<String>> params = queryStringDecoder.parameters();
    final Map<String, String> res = new HashMap<>();
    for (final String key: params.keySet()) {
      res.put(key, params.get(key).get(0));
    }

    return ComposableFutures.fromValue(res);
  }
}
