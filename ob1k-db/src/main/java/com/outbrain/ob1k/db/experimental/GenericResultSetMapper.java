package com.outbrain.ob1k.db.experimental;

import com.outbrain.ob1k.db.ResultSetMapper;
import com.outbrain.ob1k.db.TypedRowData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: aronen
 * Date: 9/22/13
 * Time: 4:36 PM
 */
public class GenericResultSetMapper implements ResultSetMapper<Map<String, Object>> {

  @Override
  public Map<String, Object> map(final TypedRowData row, final List<String> columnNames) {
    final HashMap<String, Object> rowMap = new HashMap<>();
    for (final String columnName: columnNames) {
      final Object val = row.getRaw(columnName);
      rowMap.put(columnName, val);
    }

    return rowMap;
  }
}
