package com.outbrain.ob1k.crud.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.crud.dao.ICrudAsyncDao;
import com.outbrain.ob1k.crud.model.Entities;
import kotlin.Pair;
import kotlin.ranges.IntRange;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CrudService implements ICrudService {
  private final EndpointUtils endpointUtils = new EndpointUtils();
  private final ICrudAsyncDao<JsonObject> dao;

  public CrudService(ICrudAsyncDao<JsonObject> dao) {
    this.dao = Preconditions.checkNotNull(dao);
  }

  @Override
  public ComposableFuture<Response> list(String sort, String range, String filter) {
    IntRange rng = endpointUtils.range(Optional.ofNullable(range).orElse("[0,100000]"));
    JsonObject filterJson = endpointUtils.asJson(Optional.ofNullable(filter).orElse("{}"));
    Pair<String, String> sortPair = endpointUtils.sort(Optional.ofNullable(sort).orElse("[\"id\",\"ASC\"]"));
    JsonArray ids = ids(filterJson);
    ComposableFuture<Entities<JsonObject>> entities = ids == null ?
            dao.list(rng, sortPair, filterJson) :
            dao.list(fromJsonArray(ids));
    return entities.map(new EntitiesOfJsonToResponse(rng));
  }

  @Override
  public ComposableFuture<Response> get(String id) {
    return dao.read(id).map(new AnyToResponseFunc());
  }

  @Override
  public ComposableFuture<Response> create(Request request) {
    return dao.create(endpointUtils.asJson(request.getRequestBody()).getAsJsonObject()).map(new AnyToResponseFunc());
  }

  @Override
  public ComposableFuture<Response> update(Request request) {
    return dao.update(request.getPathParam("id"), endpointUtils.asJson(request.getRequestBody()).getAsJsonObject()).map(new AnyToResponseFunc());
  }

  @Override
  public ComposableFuture<Response> delete(String id) {
    return dao.delete(id).map(new EmptyResponseFunc());
  }

  @Override
  public String resourceName() {
    return dao.resourceName();
  }

  private JsonArray ids(JsonObject jo) {
    Set<String> keySet = jo.keySet();
    if (keySet.size() != 1) {
      return null;
    }
    String key = keySet.iterator().next();
    if (!key.replace("\"", "").equals("id")) {
      return null;
    }
    JsonElement value = jo.get(key);
    return value.isJsonArray() ? value.getAsJsonArray() : null;
  }

  private List<String> fromJsonArray(JsonArray jr) {
    List<String> lst = Lists.newArrayListWithExpectedSize(jr.size());
    for (int i = 0; i < jr.size(); ++i) {
      String value = jr.get(i).getAsJsonPrimitive().getAsString();
      lst.add(value.replace("\"", ""));
    }
    return lst;
  }
}
