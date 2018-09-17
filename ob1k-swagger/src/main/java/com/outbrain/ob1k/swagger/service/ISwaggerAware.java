package com.outbrain.ob1k.swagger.service;

import io.swagger.models.Swagger;

public interface ISwaggerAware {
  void invoke(Swagger swagger, String key);
}
