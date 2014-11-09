package com.outbrain.ob1k.client.http;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.Service;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: aronen
 * Date: 6/24/13
 * Time: 8:44 PM
 */
public class ParamsService implements Service {
  public ComposableFuture<Map<String, String>> handle(final HttpRequest request) {
    final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
    final Map<String,List<String>> params = queryStringDecoder.getParameters();
    final Map<String, String> res = new HashMap<String, String>();
    for (final String key: params.keySet()) {
      res.put(key, params.get(key).get(0));
    }

    return ComposableFutures.fromValue(res);
  }
}
