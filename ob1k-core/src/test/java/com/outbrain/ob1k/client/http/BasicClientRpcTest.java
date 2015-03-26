package com.outbrain.ob1k.client.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.ctx.AsyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.SyncClientRequestContext;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.server.build.*;
import com.outbrain.ob1k.server.filters.CachingFilter;
import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.ServerBuilder;
import rx.Observable;
import rx.functions.Action1;

/**
 * User: aronen
 * Date: 8/19/13
 * Time: 5:16 PM
 */
public class BasicClientRpcTest {

  private static final String HELLO_SERVICE_PATH = "/hello-service";
  private static final String FILTERED_SERVICE_PATH = "/filtered";
  private static final String CTX_PATH = "/TestApp";

  private static int port;
  private static Server server;

  private static ServiceFilter createCachingFilter() {
    return new CachingFilter<String, String>(new CachingFilter.CacheKeyGenerator<String>() {
      @Override
      public String createKey(Object[] params) {
        return params[0].toString();
      }
    },10, 1, TimeUnit.SECONDS);
  }

  @BeforeClass
  public static void setup() {
    server = ServerBuilder.newBuilder().
        configurePorts(new PortsProvider() {
          @Override
          public void configure(final ChoosePortPhase builder) {
            builder.useRandomPort();
          }
        }).
        setContextPath(CTX_PATH).
        configureExtraParams(new ExtraParamsProvider() {
          @Override
          public void configureExtraParams(final ExtraParamsPhase builder) {
            builder.configureExecutorService(5, 10);

          }
        }).
        withServices(new RawServiceProvider() {
          @Override
          public void addServices(final AddRawServicePhase builder) {
            builder.addService(new HelloService(), HELLO_SERVICE_PATH);
            builder.addService(new ParamsService(), "/params");
            builder.defineService(new FilteredService(), FILTERED_SERVICE_PATH, new ServiceBindingProvider() {
              @Override
              public void configureService(RawServiceBuilderPhase builder) {
               builder.addEndpoint("getNextCode", "/next", createCachingFilter());
               builder.addEndpoint("getRandomCode", "/random");
              }
            });
          }
        }).
        configureStaticResources(new StaticResourcesProvider() {
          @Override
          public void configureResources(final StaticResourcesPhase builder) {
            builder.addStaticPath("/static");

          }
        }).
        build();

    final InetSocketAddress address = server.start();
    port = address.getPort();
  }

  @AfterClass
  public static void tearDown() {
    server.stop();
  }

  @Test
  public void testHelloService() {

    final IHelloService jsonClient = createClient(ContentType.JSON, port);
    final IHelloService msgPackClient = createClient(ContentType.MESSAGE_PACK, port);

    final List<TestBean> beans = createBeans();

    try {
      final ComposableFuture<List<TestBean>> result1 = jsonClient.increaseAge(beans, "programming");
      final List<TestBean> resBeans1 = result1.get();
      Assert.assertEquals(resBeans1.size(), beans.size());
      Assert.assertEquals(resBeans1.get(0).getAge(), beans.get(0).getAge() + 1);

      final ComposableFuture<List<TestBean>> result2 = msgPackClient.increaseAge(beans, "programming");
      final List<TestBean> resBeans2 = result2.get();
      Assert.assertEquals(resBeans2.size(), beans.size());
      Assert.assertEquals(resBeans2.get(0).getAge(), beans.get(0).getAge() + 1);
    } catch (InterruptedException | ExecutionException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testMultipleClientsSharedThreads() throws Exception {
    final List<IHelloService> clients = new ArrayList<>();
    final int numOfThreadsBefore = Thread.activeCount();

    for (int i=0; i< 100; i++) {
      final IHelloService client = createClient(ContentType.JSON, port);
      clients.add(client);
    }

    final int numOfThreads = Thread.activeCount();

    for (final IHelloService client : clients) {
      Clients.close(client);
    }

    Assert.assertTrue((numOfThreads - numOfThreadsBefore) < 100);

    System.out.println("walla...");
  }

  private List<TestBean> createBeans() {
    final List<String> habits = new ArrayList<>();
    habits.add("eating");
    habits.add("sleeping");
    final TestBean bean = new TestBean("haim", 39, habits);

    final List<TestBean> beans = new ArrayList<>();
    beans.add(bean);
    return beans;
  }

  @Test
  public void testTimeout() {
    final int requestTimeout = 100;
    final IHelloService fastClient = createClient(port, requestTimeout, 3);

    final long t1 = System.currentTimeMillis();
    final ComposableFuture<Boolean> sleepResult = fastClient.sleep(requestTimeout * 2);
    try {
      final Boolean bogusRes = sleepResult.get();
      final long t2 = System.currentTimeMillis();
      Assert.fail("request timeout should be thrown. res is: " + bogusRes + " time took: " + (t2 - t1));
    } catch (final ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof TimeoutException);
    } catch (final InterruptedException e) {
      Assert.fail(e.getMessage());
    }

    final long t2 = System.currentTimeMillis();
    final long period = t2 - t1;

    Assert.assertTrue("request time was too short", period >= requestTimeout);
    Assert.assertTrue("request time was too long", period < requestTimeout * 3);
  }

  @Test
  public void testStream() {
    final IHelloService client = createClient(ContentType.JSON, port);
    final int SIZE = 10;
    final Observable<String> messages = client.getMessages("moshe", SIZE, false);

    final List<String> results = new ArrayList<>();
    messages.toBlocking().forEach(new Action1<String>() {
      @Override
      public void call(final String element) {
        results.add(element);
      }
    });
    Assert.assertEquals(results.size(), SIZE);
    Assert.assertEquals(results.get(0), "hello moshe #0");

    final Observable<String> badMessages = client.getMessages("moshe", SIZE, true);
    final List<String> badResults = new ArrayList<>();

    try {
      badMessages.toBlocking().forEach(new Action1<String>() {
        @Override
        public void call(final String element) {
          badResults.add(element);
        }
      });
    } catch (final RuntimeException e) {
      Assert.assertTrue(e.getMessage().contains("last message is really bad"));
    } catch (final Exception e) {
      Assert.fail(e.getMessage());
    }

    Assert.assertEquals(badResults.size(), SIZE);
    Assert.assertEquals(badResults.get(0), "hello moshe #0");

//    final CountDownLatch latch = new CountDownLatch(1);
//    messages.subscribe(new Observer<String>() {
//      @Override
//      public void onCompleted() {
//        System.out.println("the end.");
//        latch.countDown();
//      }
//
//      @Override
//      public void onError(final Throwable e) {
//        System.out.println("got exception: " + e.toString());
//        latch.countDown();
//      }
//
//      @Override
//      public void onNext(final String element) {
//        System.out.println("element: " + element);
//      }
//    });
//
//    try {
//      latch.await();
//    } catch (final InterruptedException e) {
//      e.printStackTrace();
//    }

    System.out.println("test finished.");
  }

  @Test
  public void testSingleParamCall() {
    final IHelloService client = createClient(ContentType.JSON, port);
    try {
      final String haimMessage = client.hello("haim").get();
      Assert.assertTrue(haimMessage.contains("haim"));
    } catch (InterruptedException | ExecutionException e) {
      Assert.fail(e.getMessage());
    }

    try {
      final String moshe = client.hello("moshe").get();
      Assert.fail("method should throw exception with this param");
    } catch (final InterruptedException e) {
      Assert.fail(e.getMessage());
    } catch (final ExecutionException e) {
      Assert.assertNotNull(e.getCause());
    }

    try {
      ((Closeable) client).close();
    } catch (final IOException e) {
      Assert.fail("failed to close the client.");
    }

  }

  @Test
  public void testFilters() {
    final IHelloService client = createClientWithFilters(ContentType.JSON, port);
    try {
      final String res = client.hello("haim").get();
      Assert.assertEquals(res, "hello haim !!!");
    } catch (InterruptedException | ExecutionException e) {
      Assert.fail(e.getMessage());
    }

    try {
      final String res = client.helloNow("haim");
      Assert.assertEquals(res, "hello haim !!!");
    } catch (final Throwable e) {
      Assert.fail(e.getMessage());
    }

    try {
      final String res = client.helloFilter("haim").get();
      Assert.assertEquals(res, "hello haim ? !!!");
    } catch (final Throwable e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void testCachingFilters() throws Exception {
    final IFilteredService client = createFilteredClient(port);
    try {
      final String res1 = client.getNextCode("haim").get();
      final String res2 = client.getNextCode("haim").get();

      Thread.sleep(2000);

      final String res3 = client.getNextCode("haim").get();

      Assert.assertEquals(res1, res2);
      Assert.assertNotSame(res2, res3);

      final String res4 = client.getRandomCode("moshe").get();
      final String res5 = client.getRandomCode("moshe").get();

      Thread.sleep(2000);

      final String res6 = client.getRandomCode("moshe").get();

      Assert.assertEquals(res4, res5);
      Assert.assertNotSame(res5, res6);

    } finally {
      Clients.close(client);
    }
  }


  private IHelloService createClient(final ContentType protocol, final int port) {
    return new ClientBuilder<>(IHelloService.class).
        setProtocol(protocol).
        setRequestTimeout(120000). // heavily loaded testing environment.
        addTarget("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH).
        build();
  }

  private IFilteredService createFilteredClient(final int port) {
    return new ClientBuilder<>(IFilteredService.class).
        setProtocol(ContentType.JSON).
        addTarget("http://localhost:" + port + CTX_PATH + FILTERED_SERVICE_PATH).
        bindEndpoint("getNextCode", "/next").
        bindEndpoint("getRandomCode", "/random", createCachingFilter()).
        build();
  }

  private IHelloService createClient(final int port, final int requestTimeout, final int retries) {
    return new ClientBuilder<>(IHelloService.class).
        setProtocol(ContentType.JSON).
        setRequestTimeout(requestTimeout).
        setRetries(retries).
        addTarget("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH).
        build();
  }

  private IHelloService createClientWithFilters(final ContentType protocol, final int port) {
    return new ClientBuilder<>(IHelloService.class).
        setProtocol(protocol).
        addFilter(new BangFilter()).
        bindEndpoint("helloFilter", new QFilter()).
        setRequestTimeout(120000). // heavily loaded testing environment.
            addTarget("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH).
            build();
  }

  private final static class QFilter implements AsyncFilter<String, AsyncClientRequestContext> {
    @Override
    public ComposableFuture<String> handleAsync(final AsyncClientRequestContext ctx) {
      return ctx.<String>invokeAsync().continueOnSuccess(new FutureSuccessHandler<String, String>() {
        @Override
        public ComposableFuture<String> handle(final String result) {
          return ComposableFutures.fromValue(result + " ?");
        }
      });
    }
  }

  private final static class BangFilter implements AsyncFilter<String, AsyncClientRequestContext>, SyncFilter<String, SyncClientRequestContext> {
    @Override
    public ComposableFuture<String> handleAsync(final AsyncClientRequestContext ctx) {
      return ctx.<String>invokeAsync().continueOnSuccess(new FutureSuccessHandler<String, String>() {
        @Override
        public ComposableFuture<String> handle(final String result) {
          return ComposableFutures.fromValue(result + " !!!");
        }
      });
    }

    @Override
    public String handleSync(final SyncClientRequestContext ctx) throws ExecutionException {
      return ctx.invokeSync() + " !!!";
    }
  }

}
