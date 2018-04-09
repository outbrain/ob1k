package com.outbrain.ob1k.db;

import com.github.mauricio.async.db.QueryResult;
import com.github.mauricio.async.db.ResultSet;
import com.github.mauricio.async.db.RowData;
import com.google.common.base.Joiner;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.transform;
import static java.util.Collections.singletonList;

/**
 * @author Asy Ronen
 * @author Eran Harel
 */
public class BasicDao {
  private final DbConnectionPool _pool;

  public BasicDao(final DbConnectionPool pool) {
    this._pool = pool;
  }

  /**
   * @param query SQL query to execute
   * @param keyMapper a mapper between the the value object to the map key
   * @param valueMapper a mapper between the resultset row, and the returned value(s)
   * @param <V> map values type
   * @param <K> map keys type
   * @return a mapping of the result set mapping each row using the provided keyMapper and valueMapper
   * @throws IllegalStateException if the result set contains a duplicate key
   */
  public <K, V> ComposableFuture<Map<K, V>> map(final String query, final Function<V, K> keyMapper, final ResultSetMapper<V> valueMapper) {
    return toMap(list(query, valueMapper), keyMapper);
  }

  /**
   * @param conn connection to be used to execute the query
   * @param query SQL query to execute
   * @param keyMapper a mapper between the the value object to the map key
   * @param valueMapper a mapper between the resultset row, and the returned value(s)
   * @param <V> map values type
   * @param <K> map keys type
   * @return a mapping of the result set mapping each row using the provided keyMapper and valueMapper
   * @throws IllegalStateException if the result set contains a duplicate key
   */
  public <K, V> ComposableFuture<Map<K, V>> map(final MySqlAsyncConnection conn, final String query, final Function<V, K> keyMapper, final ResultSetMapper<V> valueMapper) {
    return toMap(list(conn, query, valueMapper), keyMapper);
  }

  private <K, V> ComposableFuture<Map<K, V>> toMap(final ComposableFuture<List<V>> list, final Function<V, K> keyMapper) {
    return list.map(result -> result.stream().
      collect(Collectors.toMap(keyMapper, Function.identity())));
  }

  /**
   * @param query SQL query to execute
   * @param keyMapper a mapper between the the value object to the map key
   * @param valueMapper a mapper between the resultset row, and the returned value(s)
   * @param <V> map values type
   * @param <K> map keys type
   * @return a mapping of the result set mapping each row using the provided keyMapper and valueMapper, grouping values by key
   */
  public <K, V> ComposableFuture<Map<K, List<V>>> group(final String query, final Function<V, K> keyMapper, final ResultSetMapper<V> valueMapper) {
    return toGroupMap(list(query, valueMapper), keyMapper);
  }

  /**
   * @param conn connection to be used to execute the query
   * @param query SQL query to execute
   * @param keyMapper a mapper between the the value object to the map key
   * @param valueMapper a mapper between the resultset row, and the returned value(s)
   * @param <V> map values type
   * @param <K> map keys type
   * @return a mapping of the result set mapping each row using the provided keyMapper and valueMapper, grouping values by key
   */
  public <K, V> ComposableFuture<Map<K, List<V>>> group(final MySqlAsyncConnection conn, final String query, final Function<V, K> keyMapper, final ResultSetMapper<V> valueMapper) {
    return toGroupMap(list(conn, query, valueMapper), keyMapper);
  }

  private <K, V> ComposableFuture<Map<K, List<V>>> toGroupMap(final ComposableFuture<List<V>> list, final Function<V, K> keyMapper) {
    return list.map(result -> result.stream().
      collect(Collectors.groupingBy(keyMapper)));
  }

  public ComposableFuture<List<Map<String, Object>>> list(final String query) {
    return list(query, new GenericResultSetMapper());
  }

  public ComposableFuture<List<Map<String, Object>>> list(final MySqlAsyncConnection conn, final String query) {
    return list(conn, query, new GenericResultSetMapper());
  }

  public <T> ComposableFuture<List<T>> list(final String query, final ResultSetMapper<T> mapper) {
    final ComposableFuture<QueryResult> queryRes = _pool.sendQuery(query);
    return _list(queryRes, mapper);
  }

  private <T> ComposableFuture<List<T>> _list(final ComposableFuture<QueryResult> queryRes, final ResultSetMapper<T> mapper) {
    return queryRes.map(res -> {
      final Option<ResultSet> rowsOption = res.rows();

      final List<T> response = new ArrayList<>();
      if (rowsOption.isDefined()) {
        final ResultSet resultSet = rowsOption.get();
        final List<String> columnNames = JavaConversions.seqAsJavaList(resultSet.columnNames());

        final Iterator<RowData> rows = resultSet.iterator();
        while (rows.hasNext()) {
          final RowData row = rows.next();
          final T obj = mapper.map(new TypedRowData(row), columnNames);
          response.add(obj);
        }
      }

      return response;
    });
  }

  public <T> ComposableFuture<List<T>> list(final MySqlAsyncConnection conn, final String query, final ResultSetMapper<T> mapper) {
    final ComposableFuture<QueryResult> queryRes = conn.sendQuery(query);
    return _list(queryRes, mapper);
  }

  public <T> ComposableFuture<List<T>> list(final String tableName, final String idColumnName, final List<?> ids, final ResultSetMapper<T> mapper) {
    return list(createListByIDsQuery(tableName, idColumnName, ids), mapper);
  }

  private String createListByIDsQuery(final String tableName, final String idColumnName, final List<?> ids) {
    return createQueryForIds("select * from", tableName, idColumnName, ids);
  }

  private String createQueryForIds(final String baseQuery, final String tableName, final String idColumnName,
                                   final List<?> ids) {
    final StringBuilder query = new StringBuilder(baseQuery);
    query.append(' ');
    query.append(withBackticks(tableName));
    query.append(" where ");
    query.append(withBackticks(idColumnName));
    query.append(" in (");
    final Joiner joiner = Joiner.on(',');
    joiner.appendTo(query, transform(ids, BasicDao::withQuote)); // todo: why it's not just List<String> ?
    query.append(");");

    return query.toString();
  }

  public <T> ComposableFuture<List<T>> list(final MySqlAsyncConnection conn, final String tableName, final String idColumnName,
                                            final List<?> ids, final ResultSetMapper<T> mapper) {
    final ComposableFuture<QueryResult> queryRes = conn.sendQuery(createListByIDsQuery(tableName, idColumnName, ids));
    return _list(queryRes, mapper);
  }

  //  public <T> ComposableFuture<T> get(final String tableName, String idColumnName, Object id, final ResultSetMapper<T> mapper) {
  //    ???
  //  }

  public <T> ComposableFuture<T> get(final String query, final ResultSetMapper<T> mapper) {
    final ComposableFuture<QueryResult> queryRes = _pool.sendQuery(query);
    return _get(queryRes, mapper);
  }

  private <T> ComposableFuture<T> _get(final ComposableFuture<QueryResult> queryRes, final ResultSetMapper<T> mapper) {
    return queryRes.map(res -> {
      final Option<ResultSet> rowsOption = res.rows();

      if (rowsOption.isDefined()) {
        final ResultSet resultSet = rowsOption.get();
        final List<String> columnNames = JavaConversions.seqAsJavaList(resultSet.columnNames());

        final Iterator<RowData> rows = resultSet.iterator();
        if (rows.hasNext()) {
          final RowData row = rows.next();
          return mapper.map(new TypedRowData(row), columnNames);
        }
      }

      return null;
    });
  }

  public <T> ComposableFuture<T> get(final MySqlAsyncConnection conn, final String query, final ResultSetMapper<T> mapper) {
    final ComposableFuture<QueryResult> queryRes = conn.sendQuery(query);
    return _get(queryRes, mapper);
  }

  public <T> ComposableFuture<T> withConnection(final TransactionHandler<T> handler) {
    return _pool.withConnection(handler);
  }

  public <T> ComposableFuture<T> withTransaction(final TransactionHandler<T> handler) {
    return _pool.withTransaction(handler);
  }

  public <T> ComposableFuture<T> get(final ResultSetMapper<T> mapper, final String tableName, final int limit) {
    return get("select * from " + withBackticks(tableName) + " limit " + limit, mapper);
  }

  public <T> ComposableFuture<T> get(final MySqlAsyncConnection conn, final ResultSetMapper<T> mapper, final String tableName, final int limit) {
    return get(conn, "select * from " + withBackticks(tableName) + " limit " + limit, mapper);
  }

  public ComposableFuture<Map<String, Object>> get(final String query) {
    return get(query, new GenericResultSetMapper());
  }

  public ComposableFuture<Map<String, Object>> get(final MySqlAsyncConnection conn, final String query) {
    return get(conn, query, new GenericResultSetMapper());
  }

  public ComposableFuture<Long> execute(final String command) {
    final ComposableFuture<QueryResult> queryRes = _pool.sendQuery(command);
    return _execute(queryRes);
  }

  public ComposableFuture<Long> executeAndGetId(final String command) {
    return withConnection(conn -> execute(conn, command)
      .flatMap(result -> get(conn, "select LAST_INSERT_ID()"))
      .map(result -> (Long) result.get("LAST_INSERT_ID()")));
  }

  public <T> ComposableFuture<Long> saveAndGetId(final T entry, final String tableName, final EntityMapper<T> mapper) {
    final String saveCommand = createSaveCommand(singletonList(entry), tableName, mapper);
    return executeAndGetId(saveCommand);
  }

  public ComposableFuture<Long> execute(final MySqlAsyncConnection conn, final String command) {
    final ComposableFuture<QueryResult> queryRes = conn.sendQuery(command);
    return _execute(queryRes);
  }

  private ComposableFuture<Long> _execute(final ComposableFuture<QueryResult> queryRes) {
    return queryRes.map(QueryResult::rowsAffected);
  }

  public ComposableFuture<Long> delete(final String tableName, final String idColumnName, final Object id) {
    return execute(createDeleteCommand(tableName, idColumnName, singletonList(id)));
  }

  public ComposableFuture<Long> delete(final MySqlAsyncConnection conn, final String tableName,
                                       final String idColumnName, final Object id) {
    return execute(conn, createDeleteCommand(tableName, idColumnName, singletonList(id)));
  }

  public ComposableFuture<Long> delete(final String tableName, final String idColumnName, final List<?> ids) {
    return execute(createDeleteCommand(tableName, idColumnName, ids));
  }

  public ComposableFuture<Long> delete(final MySqlAsyncConnection conn, final String tableName,
                                       final String idColumnName, final List<?> ids) {
    return execute(conn, createDeleteCommand(tableName, idColumnName, ids));
  }

  private String createDeleteCommand(final String tableName, final String idColumnName, final List<?> ids) {
    return createQueryForIds("delete from", tableName, idColumnName, ids);
  }

  public <T> ComposableFuture<Long> save(final List<T> entries, final String tableName, final EntityMapper<T> mapper) {
    return execute(createSaveCommand(entries, tableName, mapper));
  }

  public <T> ComposableFuture<Long> save(final MySqlAsyncConnection conn, final List<T> entries, final String tableName, final EntityMapper<T> mapper) {
    return execute(conn, createSaveCommand(entries, tableName, mapper));
  }

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
  public <T> ComposableFuture<Long> update(final String tableName, final T entry, final EntityMapper<T> entryMapper,
                                           final EntityMapper<T> idMapper) {
    return execute(createUpdateCommand(tableName, entry, entryMapper, idMapper));
  }

  /**
   * Updates entry/entries of table by provided values &amp; conditions
   *
   * @param conn connection to be used to execute the query
   * @param tableName table name to update values of
   * @param entry entry to update by
   * @param entryMapper a mapper between the the value object to the map key
   * @param idMapper a mapper between the condition column to update by to the value (.. where column=value)
   * @param <T> entry generic
   * @return number of updated entries
   */
  public <T> ComposableFuture<Long> update(final MySqlAsyncConnection conn, final String tableName, final T entry,
                                           final EntityMapper<T> entryMapper, final EntityMapper<T> idMapper) {
    return execute(conn, createUpdateCommand(tableName, entry, entryMapper, idMapper));
  }

  private <T> String createUpdateCommand(final String tableName, final T entry, final EntityMapper<T> entryMapper,
                                         final EntityMapper<T> idMapper) {
    final StringBuilder command = new StringBuilder("update ");
    command.append(withBackticks(tableName));
    command.append(" set ");

    final Map<String, Object> updateValues = entryMapper.map(entry);
    final String entriesToUpdate = toColumnValueString(updateValues, ", ");

    command.append(entriesToUpdate);
    command.append("where ");

    final Map<String, Object> updateByEntries = idMapper.map(entry);
    final String updateBy = toColumnValueString(updateByEntries, " and ");

    command.append(updateBy);
    command.append(";");

    return command.toString();
  }

  private String toColumnValueString(final Map<String, Object> updateValues, final String seperator) {
    return updateValues.entrySet().stream().map(entry ->
      withBackticks(entry.getKey()) + "=" + withQuote(entry.getValue())).
      collect(Collectors.joining(seperator));
  }

  private <T> String createSaveCommand(final List<T> entries, final String tableName, final EntityMapper<T> mapper) {
    // using the following syntax: INSERT INTO tbl_name (a,b,c) VALUES(1,2,3),(4,5,6),(7,8,9);

    if (entries == null || entries.isEmpty())
      throw new IllegalArgumentException("entries must contain at least on entry");

    final StringBuilder command = new StringBuilder("insert into ");
    command.append(withBackticks(tableName));

    // setting the columns part (col1, col2, ...)
    command.append(" (");
    final T first = entries.get(0);
    final List<String> columnNames = new ArrayList<>(mapper.map(first).keySet());
    final Joiner joiner = Joiner.on(',');
    joiner.appendTo(command, transform(columnNames, BasicDao::withBackticks));
    command.append(") ");

    command.append(" values ");

    // setting the values part values (val11, val12, ...), (val21, val22, ...), ...
    for (final T entry : entries) {
      if (entry != first)
        command.append(",");

      command.append(" (");
      final List<String> values = new ArrayList<>();
      final Map<String, Object> elements = mapper.map(entry);
      for (final String column : columnNames) {
        final Object value = elements.get(column);
        if (value != null) {
          final String queryValue = withQuote(value);
          values.add(queryValue);
        } else {
          values.add("NULL");
        }
      }

      joiner.appendTo(command, values);
      command.append(") ");
    }

    command.append(";");
    return command.toString();
  }

  public ComposableFuture<Boolean> shutdown() {
    return _pool.close();
  }

  private static String withBackticks(final String columnOrTable) {
    return "`" + columnOrTable + "`";
  }

  private static String withQuote(final Object value) {
    if (value instanceof Boolean) {
      return value.toString();
    }

    return "'" + value + "'";
  }
}
