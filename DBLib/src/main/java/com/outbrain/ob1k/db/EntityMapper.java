package com.outbrain.ob1k.db;

import java.util.Map;

/**
 * User: aronen
 * Date: 11/11/13
 * Time: 4:39 PM
 */
public interface EntityMapper<T> {
  Map<String, Object> map(T entity);
}
