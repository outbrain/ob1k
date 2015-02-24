package com.outbrain.ob1k.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.build.*;
import com.outbrain.ob1k.server.entities.OtherEntity;
import com.outbrain.ob1k.server.entities.TestEntity;
import com.outbrain.ob1k.server.services.SimpleRequestMethodTypesService;
import com.outbrain.ob1k.server.services.SimpleRequestMethodTypesServiceImpl;
import com.outbrain.ob1k.server.services.SimpleTestService;
import com.outbrain.ob1k.server.services.SimpleTestServiceImpl;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by aronen on 10/6/14.
 */
public class BasicServerRpcTest {

    @Test
    public void testServerListener() throws Exception {
        final Listener listener = new Listener();
        final Server server = buildServer(listener);
        server.addListener(listener);
        server.start();
        Assert.assertEquals("serverStarted() wasn't called", 2, listener.serverStartedCallCount);
        server.stop();
    }

    @Test
    public void testMethodSpecificRequestsWithMsgPack() throws Exception {
        Server server = null;
        SimpleRequestMethodTypesService client = null;
        try {
            server = buildServer(null);
            final int port = server.start().getPort();

            client = new ClientBuilder<>(SimpleRequestMethodTypesService.class).
                addTarget("http://localhost:" + port + "/test/users").
                bindEndpoint("getAll", HttpRequestMethodType.GET, "/").
                bindEndpoint("createUser", HttpRequestMethodType.PUT, "/").
                bindEndpoint("fetchUser", HttpRequestMethodType.GET, "/{id}").
                bindEndpoint("deleteUser", HttpRequestMethodType.DELETE, "/{id}").
                bindEndpoint("updateUser", HttpRequestMethodType.POST, "/{id}").
                bindEndpoint("mustBeGet").
                bindEndpoint("any").
                setRequestTimeout(100000).
                setProtocol(ContentType.MESSAGE_PACK).
                build();

            try {
                final List<SimpleRequestMethodTypesServiceImpl.Person> persons = client.getAll().get();
                Assert.assertTrue(persons.get(0).name.startsWith("Y"));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final String updateUser = client.updateUser(1, "Eng. Yossuf", "Java Expert").get();
                Assert.assertTrue(Objects.equals(updateUser, SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final SimpleRequestMethodTypesService.Person createUser = client.createUser("Julia", "Android Developer").get();
                Assert.assertTrue(Objects.equals(createUser.name, "Julia"));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final SimpleRequestMethodTypesServiceImpl.Person user = client.fetchUser(1).get();
                Assert.assertTrue(user.name.startsWith("Y"));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final String deleteUser = client.deleteUser(1).get();
                Assert.assertTrue(Objects.equals(deleteUser, SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                client.mustBeGet().get();
                Assert.fail("Should not succeed since sending request via post, instead of get method");
            } catch (final Exception e) {
                Assert.assertTrue(SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS, true);
            }

            try {
                client.any().get();
                Assert.assertTrue(SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS, true);
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

        } finally {
            if (client != null)
                Clients.close(client);

            if (server != null)
                server.stop();
        }
    }

    @Test
    public void testMethodSpecificRequestsWithJson() throws Exception {
        Server server = null;
        SimpleRequestMethodTypesService client = null;
        try {
            server = buildServer(null);
            final int port = server.start().getPort();

            client = new ClientBuilder<>(SimpleRequestMethodTypesService.class).
                addTarget("http://localhost:" + port + "/test/users").
                bindEndpoint("getAll", HttpRequestMethodType.GET, "/").
                bindEndpoint("createUser", HttpRequestMethodType.PUT, "/").
                bindEndpoint("fetchUser", HttpRequestMethodType.GET, "/{id}").
                bindEndpoint("deleteUser", HttpRequestMethodType.DELETE, "/{id}").
                bindEndpoint("updateUser", HttpRequestMethodType.POST, "/{id}").
                bindEndpoint("mustBeGet").
                bindEndpoint("any").
                setRequestTimeout(100000).
                setProtocol(ContentType.JSON).
                build();

            try {
                final List<SimpleRequestMethodTypesServiceImpl.Person> persons = client.getAll().get();
                Assert.assertTrue(persons.get(0).name.startsWith("Y"));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final String updateUser = client.updateUser(1, "Eng. Yossuf", "Java Expert").get();
                Assert.assertTrue(Objects.equals(updateUser, SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final SimpleRequestMethodTypesService.Person createUser = client.createUser("Julia", "Android Developer").get();
                Assert.assertTrue(Objects.equals(createUser.name, "Julia"));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final SimpleRequestMethodTypesServiceImpl.Person user = client.fetchUser(1).get();
                Assert.assertTrue(user.name.startsWith("Y"));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                final String deleteUser = client.deleteUser(1).get();
                Assert.assertTrue(Objects.equals(deleteUser, SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS));
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

            try {
                client.mustBeGet().get();
                Assert.fail("Should not succeed since sending request via post, instead of get method");
            } catch (final Exception e) {
                Assert.assertTrue(SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS, true);
            }

            try {
                client.any().get();
                Assert.assertTrue(SimpleRequestMethodTypesServiceImpl.GREAT_SUCCESS, true);
            } catch (final Exception e) {
                Assert.fail("Shouldn't have fail: " + e.getMessage());
            }

        } finally {
            if (client != null)
                Clients.close(client);

            if (server != null)
                server.stop();
        }
    }

    @Test
    public void testMaxConnections() throws Exception {
        Server server = null;
        SimpleTestService client = null;
        try {
            server = buildServer(null);
            final int port = server.start().getPort();
            client = buildClientForSimpleTestWithMaxConnections(port, 2);

            final ComposableFuture<String> resp1 = client.waitForever();
            final ComposableFuture<String> resp2 = client.waitForever();
            final ComposableFuture<String> resp3 = client.waitForever();

            try {
                resp3.get();
                Assert.fail("should never return.");
            } catch (final ExecutionException e) {
                Assert.assertTrue(e.getCause().getMessage().contains("Too many connections"));
            }

            try {
                resp1.get();
                Assert.fail("should never return.");
            } catch (final ExecutionException e) {
                Assert.assertEquals(e.getCause().getClass(), IOException.class);
            }
            try {
                resp2.get();
                Assert.fail("should never return.");
            } catch (final ExecutionException e) {
                Assert.assertEquals(e.getCause().getClass(), IOException.class);
            }

        } finally {

            if (client != null)
                Clients.close(client);

            if (server != null)
                server.stop();
        }
    }

    @Test
    public void testServiceCreation() throws Exception {
        Server server = null;
        SimpleTestService client = null;
        try {
            server = buildServer(null);
            final int port = server.start().getPort();
            client = buildClientForSimpleTest(port);

            final ComposableFuture<String> res1 =
                client.method1(3, "4", new TestEntity(Sets.newHashSet(1L, 2L, 3L), "moshe", null, Lists.<OtherEntity>newArrayList()));

            try {
                final String response1 = res1.get();
                Assert.assertTrue(response1.endsWith("moshe"));
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }

            final ComposableFuture<TestEntity> res2 = client.method2(3, "4");
            try {
                final TestEntity response2 = res2.get();
                Assert.assertEquals(response2.getOthers().get(0).getValue1(), 1);
                Assert.assertEquals(response2.getOthers().get(0).getValue2(), "2");
            } catch (final ExecutionException e) {
                e.printStackTrace();
            }
        } finally {
            if (client != null)
                Clients.close(client);

            if (server != null)
                server.stop();
        }

    }

    private SimpleTestService buildClientForSimpleTest(final int port) {
        return new ClientBuilder<>(SimpleTestService.class).addTarget("http://localhost:" + port + "/test/simple").build();
    }

    private SimpleTestService buildClientForSimpleTestWithMaxConnections(final int port, final int maxConnections) {
        return new ClientBuilder<>(SimpleTestService.class).
            setMaxConnectionsPerHost(maxConnections).
            addTarget("http://localhost:" + port + "/test/simple").
            build();
    }

    private static Server buildServer(final Listener listener) {
        return ServerBuilder.newBuilder().configurePorts(new PortsProvider() {
            @Override
            public void configure(final ChoosePortPhase builder) {
                builder.useRandomPort();
            }
        }).setContextPath("/test").withServices(new RawServiceProvider() {
            @Override
            public void addServices(final AddRawServicePhase builder) {
                builder.addService(new SimpleTestServiceImpl(), "/simple");
                builder.defineService(new SimpleRequestMethodTypesServiceImpl(), "/users", new ServiceBindingProvider() {
                    @Override
                    public void configureService(final RawServiceBuilderPhase builder) {
                        builder.addEndpoint(HttpRequestMethodType.GET, "getAll", "/");
                        builder.addEndpoint(HttpRequestMethodType.GET, "fetchUser", "/{id}");
                        builder.addEndpoint(HttpRequestMethodType.POST, "updateUser", "/{id}");
                        builder.addEndpoint(HttpRequestMethodType.DELETE, "deleteUser", "/{id}");
                        builder.addEndpoint(HttpRequestMethodType.PUT, "createUser", "/");
                        builder.addEndpoint(HttpRequestMethodType.GET, "mustBeGet", "/mustBeGet");
                        builder.addEndpoint(HttpRequestMethodType.ANY, "any", "/any");
                    }
                });
            }
        }).configureExtraParams(new ExtraParamsProvider() {
            @Override
            public void configureExtraParams(final ExtraParamsPhase builder) {
                builder.setRequestTimeout(50, TimeUnit.MILLISECONDS);
                if (listener != null) {
                    builder.addListener(listener);
                }
            }
        }).build();
    }

    @Test
    public void testSlowService() throws Exception {
        Server server = null;
        SimpleTestService client = null;
        try {
            server = buildServer(null);
            final int port = server.start().getPort();
            client = buildClientForSimpleTest(port);

            final ComposableFuture<Boolean> res = client.slowMethod(100);

            try {
                final Boolean response1 = res.get();
                Assert.fail("should get timeout exception");
            } catch (final ExecutionException e) {
                Assert.assertTrue(e.getCause().getMessage().contains("response took too long"));
            }

        } finally {
            if (client != null)
                Clients.close(client);

            if (server != null)
                server.stop();
        }
    }

    @Test
    public void testNoParamMethod() throws Exception {
        Server server = null;
        SimpleTestService client = null;
        try {
            server = buildServer(null);
            final int port = server.start().getPort();
            client = buildClientForSimpleTest(port);

            try {
                final Integer nextNum = client.nextRandom().get();
                Assert.assertTrue(nextNum != null);
            } catch (final Exception e) {
                Assert.fail("no params method failed. error: " + e.getMessage());
            }

        } finally {
            if (client != null)
                Clients.close(client);

            if (server != null)
                server.stop();
        }
    }

    private static class Listener implements Server.Listener {

        private int serverStartedCallCount = 0;

        @Override
        public void serverStarted(Server server) {
            serverStartedCallCount++;
        }
    }
}
