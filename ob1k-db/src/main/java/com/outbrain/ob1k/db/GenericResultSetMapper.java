package com.outbrain.ob1k.db;

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
  public Map<String, Object> map(TypedRowData row, List<String> columnNames) {
    HashMap<String, Object> rowMap = new HashMap<String, Object>();
    for (String columnName: columnNames) {
      Object val = row.getRaw(columnName);
      rowMap.put(columnName, val);
    }

    return rowMap;
  }
}
