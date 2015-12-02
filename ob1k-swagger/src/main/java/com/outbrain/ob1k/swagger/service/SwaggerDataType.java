package com.outbrain.ob1k.swagger.service;

public enum SwaggerDataType {

  INT("integer"),
  INTEGER("integer"),
  LONG("integer"),
  FLOAT("number"),
  DOUBLE("number"),
  STRING("string"),
  BYTE("string"),
  BOOLEAN("boolean"),
  DATE("date"),
  DATETIME("date-time");

  private final String dataType;


  SwaggerDataType(final String dataType) {
    this.dataType = dataType;
  }

  public static String forClass(Class clazz) {
    final SwaggerDataType dataType;
    try {
      return SwaggerDataType.valueOf(clazz.getSimpleName().toUpperCase()).dataType;
    } catch (IllegalArgumentException e) {
      return "undefined";
    }
  }

}
