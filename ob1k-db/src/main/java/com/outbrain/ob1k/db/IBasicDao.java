package com.outbrain.ob1k.db;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface IBasicDao {
  /**
   * @param query SQL query to execute
   * @param keyMapper a mapper between the the value object to the map key
   * @param valueMapper a mapper between the resultset row, and the returned value(s)
   * @param <V> map values type
   * @param <K> map keys type
   * @return a mapping of the result set mapping each row using the provided keyMapper and valueMapper
   * @throws IllegalStateException if the result set contains a duplicate key
   */
  <K, V> ComposableFuture<Map<K, V>> map(String query, Function<V, K> keyMapper, ResultSetMapper<V> valueMapper);


  /**
   * @param query SQL query to execute
   * @param keyMapper a mapper between the the value object to the map key
   * @param valueMapper a mapper between the resultset row, and the returned value(s)
   * @param <V> map values type
   * @param <K> map keys type
   * @return a mapping of the result set mapping each row using the provided keyMapper and valueMapper, grouping values by key
   */
  <K, V> ComposableFuture<Map<K, List<V>>> group(String query, Function<V, K> keyMapper, ResultSetMapper<V> valueMapper);

  ComposableFuture<List<Map<String, Object>>> list(String query);

  <T> ComposableFuture<List<T>> list(String query, ResultSetMapper<T> mapper);

  <T> ComposableFuture<List<T>> list(String tableName, String idColumnName, List<?> ids, ResultSetMapper<T> mapper);

  <T> ComposableFuture<T> get(String query, ResultSetMapper<T> mapper);

  <T> ComposableFuture<T> get(ResultSetMapper<T> mapper, String tableName, int limit);

  ComposableFuture<Map<String, Object>> get(String query);

  ComposableFuture<Long> execute(String command);

  ComposableFuture<Long> executeAndGetId(String command);

  <T> ComposableFuture<Long> saveAndGetId(T entry, String tableName, EntityMapper<T> mapper);

  ComposableFuture<Long> delete(String tableName, String idColumnName, Object id);

  ComposableFuture<Long> delete(String tableName, String idColumnName, List<?> ids);

  <T> ComposableFuture<Long> save(List<T> entries, String tableName, EntityMapper<T> mapper);

  /**
   * Updates entry/entries of table by provided values &amp; conditions
   *
   * @param tableName table name to update values of
   * @param entry entry to update by
   * @param entryMapper a mapper between the the value object to the map key
   * @param idMapper a mapper between the condition column to update by to the value (.. where column=value)
   * @param <T> entry generic
   * @return number of updated entries
   */
  <T> ComposableFuture<Long> update(String tableName, T entry, EntityMapper<T> entryMapper,
                                    EntityMapper<T> idMapper);


  ComposableFuture<Boolean> shutdown();
}
