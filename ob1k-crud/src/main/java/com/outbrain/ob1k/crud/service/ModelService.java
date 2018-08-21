package com.outbrain.ob1k.crud.service;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.crud.model.Model;

public class ModelService implements Service {
  private final Model model;

  public ModelService(Model model) {
    this.model = Preconditions.checkNotNull(model);
  }

  public ComposableFuture<Model> getModel() {
    return ComposableFutures.fromValue(model);
  }
}
