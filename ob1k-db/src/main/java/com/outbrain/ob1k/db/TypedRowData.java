package com.outbrain.ob1k.db;

import org.joda.time.LocalDateTime;

public abstract class TypedRowData {
  public abstract Integer getInt(int column);

  public abstract Integer getInt(String column);

  public abstract Long getLong(int column);

  public abstract Long getLong(String column);

  public abstract Boolean getBoolean(int column);

  public abstract Boolean getBoolean(String column);

  public abstract Byte getByte(String column);

  public abstract Byte getByte(int column);

  public abstract LocalDateTime getDate(int column);

  public abstract LocalDateTime getDate(String column);

  public abstract Float getFloat(int column);

  public abstract Float getFloat(String column);

  public abstract Double getDouble(int column);

  public abstract Double getDouble(String column);

  public abstract String getString(int column);

  public abstract String getString(String column);

  public abstract Object getRaw(int column);

  public abstract Object getRaw(String column);
}
