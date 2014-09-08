package com.outbrain.ob1k.db;

import org.joda.time.LocalDateTime;

import com.github.mauricio.async.db.RowData;

/**
 * User: aronen
 * Date: 10/1/13
 * Time: 2:11 PM
 */
public class TypedRowData {
  private final RowData row;

  public TypedRowData(final RowData row) {
    this.row = row;
  }

  public Integer getInt(final int column) {
    return (Integer) row.apply(column);
  }

  public Integer getInt(final String column) {
    return (Integer) row.apply(column);
  }

  public Long getLong(final int column) {
    return (Long) row.apply(column);
  }

  public Long getLong(final String column) {
    return (Long) row.apply(column);
  }

  public Boolean getBoolean(final int column) {
    return ((byte[])row.apply(column))[0] == 1;
  }

  public Boolean getBoolean(final String column) {
    final Object rawValue = row.apply(column);
    if (rawValue instanceof byte[]) {
      return ((byte[]) rawValue)[0] == 1;
    } else {
      return ((byte)rawValue) == 1;
    }
  }

  public Byte getByte(final String column) {
    return (Byte) row.apply(column);
  }

  public Byte getByte(final int column) {
    return (Byte) row.apply(column);
  }

  public LocalDateTime getDate(final int column) {
    return (LocalDateTime) row.apply(column);
  }

  public LocalDateTime getDate(final String column) {
    return (LocalDateTime) row.apply(column);
  }

  public Float getFloat(final int column) {
    return (Float)row.apply(column);
  }

  public Float getFloat(final String column) {
    return (Float)row.apply(column);
  }

  public Double getDouble(final int column) {
    return (Double)row.apply(column);
  }

  public Double getDouble(final String column) {
    return (Double)row.apply(column);
  }

  public String getString(final int column) {
    return (String) row.apply(column);
  }

  public String getString(final String column) {
    return (String) row.apply(column);
  }

  public Object getRaw(final int column) {
    return row.apply(column);
  }

  public Object getRaw(final String column) {
    return row.apply(column);
  }

}
