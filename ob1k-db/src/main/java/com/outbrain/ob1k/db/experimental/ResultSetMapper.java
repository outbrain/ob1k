package com.outbrain.ob1k.db.experimental;

import java.util.List;

/**
 * User: aronen
 * Date: 9/22/13
 * Time: 4:30 PM
 */
public interface ResultSetMapper<T> {
  T map(TypedRowData row, List<String> columnNames);
}
