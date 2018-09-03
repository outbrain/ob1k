package com.outbrain.ob1k.db.experimental;

import com.github.jasync.sql.db.RowData;
import org.joda.time.LocalDateTime;

import java.util.Objects;

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
    return (Integer) row.get(column);
  }

  public Integer getInt(final String column) {
    return (Integer) row.get(column);
  }

  public Long getLong(final int column) {
    return (Long) row.get(column);
  }

  public Long getLong(final String column) {
    return (Long) row.get(column);
  }

  public Boolean getBoolean(final int column) {
    return ((byte[]) Objects.requireNonNull(row.get(column)))[0] == 1;
  }

  public Boolean getBoolean(final String column) {
    final Object rawValue = row.get(column);
    if (rawValue instanceof byte[]) {
      return ((byte[]) rawValue)[0] == 1;
    } else {
      return ((byte)rawValue) == 1;
    }
  }

  public Byte getByte(final String column) {
    return (Byte) row.get(column);
  }

  public Byte getByte(final int column) {
    return (Byte) row.get(column);
  }

  public LocalDateTime getDate(final int column) {
    return (LocalDateTime) row.get(column);
  }

  public LocalDateTime getDate(final String column) {
    return (LocalDateTime) row.get(column);
  }

  public Float getFloat(final int column) {
    return (Float)row.get(column);
  }

  public Float getFloat(final String column) {
    return (Float)row.get(column);
  }

  public Double getDouble(final int column) {
    return (Double)row.get(column);
  }

  public Double getDouble(final String column) {
    return (Double)row.get(column);
  }

  public String getString(final int column) {
    return (String) row.get(column);
  }

  public String getString(final String column) {
    return (String) row.get(column);
  }

  public Object getRaw(final int column) {
    return row.get(column);
  }

  public Object getRaw(final String column) {
    return row.get(column);
  }

}
