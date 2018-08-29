package com.outbrain.ob1k.crud.service;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

public interface ICrudService extends Service {
  ComposableFuture<Response> list(String sort, String range, String filter);

  ComposableFuture<Response> get(String id);

  ComposableFuture<Response> create(Request request);

  ComposableFuture<Response> update(Request request);

  ComposableFuture<Response> delete(String id);

  String resourceName();
}
