package com.outbrain.ob1k.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.outbrain.swinfra.metrics.api.MetricFactory;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;

import com.github.mauricio.async.db.QueryResult;
import com.github.mauricio.async.db.ResultSet;
import com.github.mauricio.async.db.RowData;
import com.google.common.base.Joiner;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;

/**
 * User: aronen
 * Date: 9/22/13
 * Time: 4:15 PM
 */
public class BasicDao {
  private final MySqlConnectionPool _pool;

  public BasicDao(final MySqlConnectionPool pool) {
    this._pool = pool;
  }

  /**
   * Warning - Use only when the connection string is in host:port format without schema and other parameter.
   * example: devdb.il.outbrain.com:3308
   * @param connectionString - host:port
   * @param database the database schema name.
   * @param userName user name for login.
   * @param password password for login.
   */
  public BasicDao(final String connectionString, final String database, final String userName, final String password, final MetricFactory metricFactory) {
    final String[] arr = connectionString.split(":");
    final String host = arr[0];
    final int port = Integer.parseInt(arr[1]);
    _pool = new MySqlConnectionPool(host, port, database, userName, password, metricFactory);
  }

  public BasicDao(final String connectionString, final String database, final String userName, final String password) {
    this(connectionString, database, userName, password, null);
  }

  /**
   * Warning - Use only when the connection string is in host:port format without schema and other parameter.
   * example: devdb.il.outbrain.com:3308
   * @param connectionString - host:port
   * @param database the database schema name.
   * @param userName user name for login.
   * @param password password for login.
   */
  public BasicDao(final String connectionString, final String database, final String userName, final String password,
                  final int maxConnections, final MetricFactory metricFactory) {
    final String[] arr = connectionString.split(":");
    final String host = arr[0];
    final int port = Integer.parseInt(arr[1]);
    _pool = new MySqlConnectionPool(host, port, database, userName, password, maxConnections, metricFactory);
  }

  public BasicDao(final String connectionString, final String database, final String userName, final String password,
                  final int maxConnections) {
    this(connectionString, database, userName, password, maxConnections, null);
  }

  public BasicDao(final String host, final int port, final String database, final String userName, final String password,
                  final int maxConnections, final MetricFactory metricFactory) {
    _pool = new MySqlConnectionPool(host, port, database, userName, password, maxConnections, metricFactory);
  }

  public BasicDao(final String host, final int port, final String database, final String userName, final String password,
                  final MetricFactory metricFactory) {
    _pool = new MySqlConnectionPool(host, port, database, userName, password, metricFactory);
  }

  public BasicDao(final String host, final int port, final String database, final String userName, final String password) {
    this(host, port, database, userName, password, null);
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
    return queryRes.continueOnSuccess(new SuccessHandler<QueryResult, List<T>>() {
      @Override
      public List<T> handle(final QueryResult res) throws ExecutionException {
        final Option<ResultSet> rowsOption = res.rows();

        final List<T> response = new ArrayList<>();
        if (rowsOption.isDefined()) {
          final ResultSet resultSet = rowsOption.get();
          final List<String> columnNames = JavaConversions.asJavaList(resultSet.columnNames());

          final Iterator<RowData> rows = resultSet.iterator();
          while (rows.hasNext()) {
            final RowData row = rows.next();
            final T obj = mapper.map(new TypedRowData(row), columnNames);
            response.add(obj);
          }
        }

        return response;
      }
    });
  }

  public <T> ComposableFuture<List<T>> list(final MySqlAsyncConnection conn, final String query, final ResultSetMapper<T> mapper) {
    final ComposableFuture<QueryResult> queryRes = conn.sendQuery(query);
    return _list(queryRes, mapper);
  }

  public <T> ComposableFuture<List<T>> list(final String tableName, final String idColumnName, final List<Object> ids, final ResultSetMapper<T> mapper) {
    return list(createListByIDsQuery(tableName, idColumnName, ids), mapper);
  }

  private String createListByIDsQuery(final String tableName, final String idColumnName, final List<Object> ids) {
    final StringBuilder query = new StringBuilder("select * from ");
    query.append(tableName);
    query.append(" where ");
    query.append(idColumnName);
    query.append(" in (");
    final Joiner joiner = Joiner.on(',');
    joiner.appendTo(query, ids);
    query.append(");");

    return query.toString();
  }

  public <T> ComposableFuture<List<T>> list(final MySqlAsyncConnection conn, final String tableName, final String idColumnName,
      final List<Object> ids, final ResultSetMapper<T> mapper) {
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
    return queryRes.continueOnSuccess(new SuccessHandler<QueryResult, T>() {
      @Override
      public T handle(final QueryResult res) throws ExecutionException {
        final Option<ResultSet> rowsOption = res.rows();

        if (rowsOption.isDefined()) {
          final ResultSet resultSet = rowsOption.get();
          final List<String> columnNames = JavaConversions.asJavaList(resultSet.columnNames());

          final Iterator<RowData> rows = resultSet.iterator();
          if (rows.hasNext()) {
            final RowData row = rows.next();
            return mapper.map(new TypedRowData(row), columnNames);
          }
        }

        return null;
      }
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
    return get("select * from " + tableName + " limit " + limit, mapper);
  }

  public <T> ComposableFuture<T> get(final MySqlAsyncConnection conn, final ResultSetMapper<T> mapper, final String tableName, final int limit) {
    return get(conn, "select * from " + tableName + " limit " + limit, mapper);
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
    return withConnection(new TransactionHandler<Long>() {
      @Override
      public ComposableFuture<Long> handle(final MySqlAsyncConnection conn) {
        return execute(conn, command).continueOnSuccess(new FutureSuccessHandler<Long, Map<String, Object>>() {
          @Override
          public ComposableFuture<Map<String, Object>> handle(final Long result) {
            return get(conn, "select LAST_INSERT_ID()");
          }
        }).continueOnSuccess(new SuccessHandler<Map<String, Object>, Long>() {
          @Override
          public Long handle(final Map<String, Object> result) throws ExecutionException {
            return (Long) result.get("LAST_INSERT_ID()");
          }
        });
      }
    });
  }

  public ComposableFuture<Long> execute(final MySqlAsyncConnection conn, final String command) {
    final ComposableFuture<QueryResult> queryRes = conn.sendQuery(command);
    return _execute(queryRes);
  }

  private ComposableFuture<Long> _execute(final ComposableFuture<QueryResult> queryRes) {
    return queryRes.continueOnSuccess(new SuccessHandler<QueryResult, Long>() {
      @Override
      public Long handle(final QueryResult res) throws ExecutionException {
        return res.rowsAffected();
      }
    });
  }

  public ComposableFuture<Long> delete(final String tableName, final String idColumnName, final Object id) {
    return execute("delete from " + tableName + " where " + idColumnName + " = " + id);
  }

  public ComposableFuture<Long> delete(final MySqlAsyncConnection conn, final String tableName, final String idColumnName, final Object id) {
    return execute(conn, "delete from " + tableName + " where " + idColumnName + " = " + id);
  }

  public ComposableFuture<Long> delete(final String tableName, final String idColumnName, final List<Object> ids) {
    final Joiner joiner = Joiner.on(',');
    final StringBuilder builder = new StringBuilder();
    builder.append("[");
    joiner.appendTo(builder, ids);
    builder.append("]");

    return execute("delete from " + tableName + " where " + idColumnName + " in " + builder.toString());
  }

  public <T> ComposableFuture<Long> save(final List<T> entries, final String tableName, final EntityMapper<T> mapper) {
    return execute(createSaveCommand(entries, tableName, mapper));
  }

  private <T> String createSaveCommand(final List<T> entries, final String tableName, final EntityMapper<T> mapper) {
    // using the following syntax: INSERT INTO tbl_name (a,b,c) VALUES(1,2,3),(4,5,6),(7,8,9);

    if (entries == null || entries.isEmpty())
      throw new IllegalArgumentException("entries must contain at least on entry");

    final StringBuilder command = new StringBuilder("insert into ");
    command.append(tableName);

    // setting the columns part (col1, col2, ...)
    command.append("(");
    final T first = entries.get(0);
    final Set<String> columns = mapper.map(first).keySet();
    final Joiner joiner = Joiner.on(',');
    joiner.appendTo(command, columns);
    command.append(") ");

    command.append(" values ");

    // setting the values part values (val11, val12, ...), (val21, val22, ...), ...
    for (final T entry : entries) {
      if (entry != first)
        command.append(",");

      command.append(" (");
      joiner.appendTo(command, mapper.map(entry).values());
      command.append(") ");
    }

    command.append(";");
    return command.toString();
  }

  public <T> ComposableFuture<Long> save(final MySqlAsyncConnection conn, final List<T> entries, final String tableName, final EntityMapper<T> mapper) {
    return execute(conn, createSaveCommand(entries, tableName, mapper));
  }

  public ComposableFuture<Boolean> shutdown() {
    return _pool.close();
  }
}
