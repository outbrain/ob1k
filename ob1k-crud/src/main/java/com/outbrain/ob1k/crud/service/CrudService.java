package com.outbrain.ob1k.crud.service;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.crud.dao.ICrudAsyncDao;
import kotlin.ranges.IntRange;

import java.util.Optional;

public class CrudService implements ICrudService {
  private final EndpointUtils endpointUtils = new EndpointUtils();
  private final ICrudAsyncDao<JsonObject> dao;

  public CrudService(ICrudAsyncDao<JsonObject> dao) {
    this.dao = Preconditions.checkNotNull(dao);
  }

  @Override
  public ComposableFuture<Response> list(String sort, String range, String filter) {
    IntRange rng = endpointUtils.range(Optional.ofNullable(range).orElse("[0,100000]"));
    return dao.list(rng,
            endpointUtils.sort(Optional.ofNullable(sort).orElse("[\"id\",\"ASC\"]")),
            endpointUtils.asJson(Optional.ofNullable(filter).orElse("{}"))).
            map(new EntitiesOfJsonToResponse(rng));
  }

  @Override
  public ComposableFuture<Response> get(int id) {
    return dao.read(id).map(new AnyToResponseFunc());
  }

  @Override
  public ComposableFuture<Response> create(Request request) {
    return dao.create(endpointUtils.asJson(request.getRequestBody()).getAsJsonObject()).map(new AnyToResponseFunc());
  }

  @Override
  public ComposableFuture<Response> update(Request request) {
    return dao.update(Integer.parseInt(request.getPathParam("id")), endpointUtils.asJson(request.getRequestBody()).getAsJsonObject()).map(new AnyToResponseFunc());
  }

  @Override
  public ComposableFuture<Response> delete(int id) {
    return dao.delete(id).map(new EmptyResponseFunc());
  }

  @Override
  public String resourceName() {
    return dao.resourceName();
  }
}
