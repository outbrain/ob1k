package com.outbrain.ob1k.db;

import com.github.mauricio.async.db.RowData;
import org.joda.time.LocalDateTime;

/**
 * User: aronen
 * Date: 10/1/13
 * Time: 2:11 PM
 */
public class TypedRowDataImpl implements TypedRowData {
  private final RowData row;

  public TypedRowDataImpl(final RowData row) {
    this.row = row;
  }

  @Override
  public Integer getInt(final int column) {
    return (Integer) row.apply(column);
  }

  @Override
  public Integer getInt(final String column) {
    return (Integer) row.apply(column);
  }

  @Override
  public Long getLong(final int column) {
    return (Long) row.apply(column);
  }

  @Override
  public Long getLong(final String column) {
    return (Long) row.apply(column);
  }

  @Override
  public Boolean getBoolean(final int column) {
    return ((byte[])row.apply(column))[0] == 1;
  }

  @Override
  public Boolean getBoolean(final String column) {
    final Object rawValue = row.apply(column);
    if (rawValue instanceof byte[]) {
      return ((byte[]) rawValue)[0] == 1;
    } else {
      return ((byte)rawValue) == 1;
    }
  }

  @Override
  public Byte getByte(final String column) {
    return (Byte) row.apply(column);
  }

  @Override
  public Byte getByte(final int column) {
    return (Byte) row.apply(column);
  }

  @Override
  public LocalDateTime getDate(final int column) {
    return (LocalDateTime) row.apply(column);
  }

  @Override
  public LocalDateTime getDate(final String column) {
    return (LocalDateTime) row.apply(column);
  }

  @Override
  public Float getFloat(final int column) {
    return (Float)row.apply(column);
  }

  @Override
  public Float getFloat(final String column) {
    return (Float)row.apply(column);
  }

  @Override
  public Double getDouble(final int column) {
    return (Double)row.apply(column);
  }

  @Override
  public Double getDouble(final String column) {
    return (Double)row.apply(column);
  }

  @Override
  public String getString(final int column) {
    return (String) row.apply(column);
  }

  @Override
  public String getString(final String column) {
    return (String) row.apply(column);
  }

  @Override
  public Object getRaw(final int column) {
    return row.apply(column);
  }

  @Override
  public Object getRaw(final String column) {
    return row.apply(column);
  }

}
