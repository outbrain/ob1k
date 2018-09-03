package com.outbrain.ob1k.db;

import org.joda.time.LocalDateTime;

public interface TypedRowData {
  Integer getInt(int column);

  Integer getInt(String column);

  Long getLong(int column);

  Long getLong(String column);

  Boolean getBoolean(int column);

  Boolean getBoolean(String column);

  Byte getByte(String column);

  Byte getByte(int column);

  LocalDateTime getDate(int column);

  LocalDateTime getDate(String column);

  Float getFloat(int column);

  Float getFloat(String column);

  Double getDouble(int column);

  Double getDouble(String column);

  String getString(int column);

  String getString(String column);

  Object getRaw(int column);

  Object getRaw(String column);
}
