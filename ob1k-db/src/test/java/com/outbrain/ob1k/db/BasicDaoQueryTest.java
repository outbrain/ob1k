package com.outbrain.ob1k.db;

import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.concurrent.handlers.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * User: aronen
 * Date: 9/23/13
 * Time: 12:59 PM
 */
@Ignore
public class BasicDaoQueryTest {

    public static class Deployment {
        public Long id;
        public Long modulesRevision;
        public String source;
        public String status;
        public boolean archived;
        public boolean fakeBool;
        public boolean prepareOnly;
        public int jsonScmRevision;
        public String buildDescription;
    }

    public static class DeploymentMapper implements ResultSetMapper<Deployment>, EntityMapper<Deployment> {
        @Override
        public Deployment map(final TypedRowData row, final List<String> columnNames) {
            final Deployment res = new Deployment();
            res.id = row.getLong("id");
            res.modulesRevision = row.getLong("modulesRevision");
            res.source = row.getString("source");
            res.status = row.getString("status");
            res.archived = row.getBoolean("archived");
            res.fakeBool = row.getBoolean("fakeBool");
            res.buildDescription = row.getString("buildDescription");
            res.jsonScmRevision = row.getInt("jsonScmRevision");
            res.prepareOnly = row.getBoolean("prepareOnly");
            return res;
        }

        @Override
        public Map<String, Object> map(final Deployment entity) {
            final Map<String, Object> res = new HashMap<>();
            res.put("modulesRevision", entity.modulesRevision);
            res.put("source", entity.source);
            res.put("status", entity.status);
            res.put("archived", entity.archived);
            res.put("fakeBool", entity.fakeBool);
            res.put("buildDescription", entity.buildDescription);
            res.put("jsonScmRevision", entity.jsonScmRevision);
            res.put("prepareOnly", entity.prepareOnly);

            return res;
        }
    }

    @Test
    public void testSimpleQuery() throws ExecutionException, InterruptedException {
        final BasicDao dao = new BasicDao(MySqlConnectionPoolBuilder.newBuilder("localhost", 3306, "aronen").forDatabase("test").build());
        final ComposableFuture<List<Map<String, Object>>> deployments = dao.list("select id,archived, source from Deployments");

        deployments.consume(new Consumer<List<Map<String, Object>>>() {
            @Override
            public void consume(final Try<List<Map<String, Object>>> results) {
                if (!results.isSuccess()) {
                    System.out.println("got error: ");
                    results.getError().printStackTrace();
                } else {
                    int index = 0;
                    for (final Map<String, Object> line : results.getValue()) {
                        System.out.println("row " + index++);
                        for (final String column : line.keySet()) {
                            System.out.println(" column: " + column + " value: " + line.get(column));
                        }
                    }
                }
            }
        });

        try {
            deployments.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        dao.shutdown().get();
        System.out.println("Done.");
    }

    @Test
    public void testSimpleQueryWithMapping() throws ExecutionException, InterruptedException {
        final BasicDao dao = new BasicDao(MySqlConnectionPoolBuilder.newBuilder("localhost", 3306, "aronen").forDatabase("test").build());
        final ComposableFuture<List<Deployment>> deployments = dao.list("select id,modulesRevision,archived,source,status from Deployments WHERE id >= 0", new DeploymentMapper());

        deployments.consume(new Consumer<List<Deployment>>() {
            @Override
            public void consume(final Try<List<Deployment>> results) {
                if (!results.isSuccess()) {
                    results.getError().printStackTrace();
                } else {
                    int index = 0;
                    for (final Deployment line : results.getValue()) {
                        System.out.println("row " + index++);
                        System.out.println(" id: " + line.id);
                        System.out.println(" source: " + line.source);
                        System.out.println(" status: " + line.status);
                        System.out.println(" archived: " + line.archived);
                        System.out.println(" modulesRevision: " + line.modulesRevision);
                        System.out.println(" fakeBool: " + line.fakeBool);
                    }
                }
            }
        });

        try {
            deployments.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        dao.shutdown().get();
        System.out.println("Done.");
    }

    @Test
    public void testListWithMapping() throws ExecutionException, InterruptedException {
        final BasicDao dao = new BasicDao(MySqlConnectionPoolBuilder.newBuilder("localhost", 3306, "aronen").forDatabase("test").build());
        final ComposableFuture<List<Deployment>> deployments = dao.list("select * from Deployments", new DeploymentMapper());
        deployments.consume(new Consumer<List<Deployment>>() {
            @Override
            public void consume(final Try<List<Deployment>> results) {
                if (!results.isSuccess()) {
                    results.getError().printStackTrace();
                } else {
                    int index = 0;
                    for (final Deployment line : results.getValue()) {
                        System.out.println("row " + index++);
                        System.out.println(" id: " + line.id);
                        System.out.println(" source: " + line.source);
                        System.out.println(" status: " + line.status);
                        System.out.println(" archived: " + line.archived);
                        System.out.println(" modulesRevision: " + line.modulesRevision);
                        System.out.println(" fakeBool: " + line.fakeBool);
                    }
                }
            }
        });

        try {
            deployments.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        dao.shutdown().get();
        System.out.println("Done.");
    }

    @Test
    public void testSimpleUpdateQuery() throws ExecutionException, InterruptedException {
        final BasicDao dao = new BasicDao(MySqlConnectionPoolBuilder.newBuilder("localhost", 3306, "aronen").forDatabase("test").build());
        final ComposableFuture<Long> rowsEffected = dao.execute("update Deployments set SOURCE = concat('*', SOURCE) where id = 1");
        rowsEffected.consume(new Consumer<Long>() {
            @Override
            public void consume(final Try<Long> rows) {
                if (rows.isSuccess()) {
                    System.out.println(rows.getValue() + " rows were effected.");
                } else {
                    rows.getError().printStackTrace();
                }
            }
        });

        try {
            rowsEffected.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        dao.shutdown().get();
        System.out.println("Done.");
    }

    public ComposableFuture<Deployment> getDeploymentBySource(final BasicDao dao, final String sourceId) {
        return dao.get("select * from Deployments where source='" + sourceId + "' limit 1", new DeploymentMapper());
    }

    public ComposableFuture<Long> insertDeployment(final BasicDao dao, final long id, final String source) {
        return dao.execute("insert into Deployments SET id=" + id + ", source='" + source + "', archived=false, prepareOnly=false, jsonScmRevision=100");
    }

    @Ignore
    @Test
    public void testGetDeploymentBySource() {
        try (final BasicTestingDao dao = new BasicTestingDao("localhost", 3306, "test", "aronen", null, 2000/*msec*/,10000 /*msec*/)) {
            final Deployment deployment =
                insertDeployment(dao, 666L, "test123").
                    continueOnSuccess(new FutureSuccessHandler<Long, Deployment>() {
                        @Override
                        public ComposableFuture<Deployment> handle(final Long result) {
                            if (result == 0)
                                return ComposableFutures.fromError(new RuntimeException("row wasn't inserted into table."));

                            return getDeploymentBySource(dao, "test123");
                        }
                    }).get();

            Assert.assertTrue(deployment != null);
            Assert.assertTrue(deployment.id.equals(666L));

        } catch (final Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void testInsertAndGetId() throws Exception {
//    final BasicDao dao = new BasicDao("localhost", 3306, "test", "aronen", null);
        final BasicDao dao = new BasicTestingDao("localhost", 3306, "test", "aronen", null, 2000/*msec*/,10000/*msec*/);
        final ComposableFuture<Map<String, Object>> resMap = dao.get("select LAST_INSERT_ID()");

        final Object res = dao.executeAndGetId("insert into Deployments set source='test125', archived=false, prepareOnly=false, jsonScmRevision=100").get();
        System.out.println("res is: " + res);
    }

    @Test
    @Ignore
    public void testSaveAndGetId() throws Exception {
        final BasicDao dao = new BasicDao(MySqlConnectionPoolBuilder.newBuilder("localhost", 3306, "aronen").forDatabase("test").build());
        final Deployment deployment = new Deployment();
        deployment.source = "test125";
        deployment.archived = false;
        deployment.prepareOnly = false;
        deployment.jsonScmRevision = 100;
        deployment.buildDescription = "testing saveAndGetId";
        deployment.jsonScmRevision = 24;

        // "insert into Deployments set source='test125', archived=false, prepareOnly=false, jsonScmRevision=100"
        final Long id = dao.saveAndGetId(deployment, "Deployments", new DeploymentMapper()).get();
        System.out.println("id is: " + id);
    }

    @Test
    public void testTransaction() {
        final BasicDao dao = new BasicDao(MySqlConnectionPoolBuilder.newBuilder("localhost", 3306, "aronen").forDatabase("test").build());

        final ComposableFuture<Boolean> finalRes = dao.withTransaction(new TransactionHandler<Boolean>() {
            @Override
            public ComposableFuture<Boolean> handle(final MySqlAsyncConnection conn) {
                final ComposableFuture<Long> futureRes1 = dao.execute(conn,
                    "insert into Deployments set archived=true, jsonScmRevision=1, prepareOnly=false,source='asy1'");

                final ComposableFuture<Long> futureRes2 = futureRes1.continueOnSuccess(new FutureSuccessHandler<Long, Long>() {
                    @Override
                    public ComposableFuture<Long> handle(final Long result) {
                        return dao.execute(conn,
                            "insert into Deployments set archived=true, jsonScmRevision=1, prepareOnly=false,source='asy2'");
                    }
                });

                return futureRes2.continueOnSuccess(new FutureSuccessHandler<Long, Boolean>() {
                    @Override
                    public ComposableFuture<Boolean> handle(final Long result) {
                        return ComposableFutures.fromValue(result == 1);
                    }
                });
            }
        });

        try {
            final Boolean res = finalRes.get(1000, TimeUnit.MILLISECONDS);
            System.out.println("res is: " + res);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            dao.shutdown();
        }

    }

    @Test
    public void testBadQuery() throws Exception {
        final BasicDao dao = new BasicDao(MySqlConnectionPoolBuilder.newBuilder("localhost", 3306, "aronen").forDatabase("test").build());
        try {
            dao.list("select * from bad_table_name").get();
        } catch (final ExecutionException e) {
            final String message = e.getCause().getMessage();
            System.out.println("message: " + message);
        }
    }
}
