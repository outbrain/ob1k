package com.outbrain.service;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.Date;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

@Api(value = "AnnotatedDummyService", description = "an annotated test service")
public class AnnotatedDummyService implements Service {

  @ApiOperation(value = "date", response = Date.class, notes = "date notes")
  public ComposableFuture<Date> date(@ApiParam(value = "millis since epoch", name = "millis")
                                       final Long millis) {
    return fromValue(new Date(millis));
  }
}
