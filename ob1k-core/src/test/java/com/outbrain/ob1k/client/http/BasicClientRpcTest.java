package com.outbrain.ob1k.client.http;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.ctx.AsyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.SyncClientRequestContext;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.server.build.*;
import com.outbrain.ob1k.server.filters.CachingFilter;
import org.junit.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.http.common.ContentType;
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
    }, 10, 1, TimeUnit.SECONDS);
  }

  private interface HelloServiceClient extends Service {
    ComposableFuture<Response> helloWorld();
    Observable<Response> getMessages(String name, int iterations, boolean failAtEnd);
  }

  private interface HelloServiceTypedClient extends Service {
    ComposableFuture<TypedResponse<String>> helloWorld();
    Observable<TypedResponse<String>> getMessages(String name, int iterations, boolean failAtEnd);
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
  public void testSimpleResponse() throws Exception {
    final HelloServiceClient helloServiceClient = new ClientBuilder<>(HelloServiceClient.class).
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH)).
            build();

    final Response response = helloServiceClient.helloWorld().get();
    Assert.assertTrue("response should contain hello world", response.getResponseBody().contains("hello world"));
  }

  @Test
  public void testSimpleResponseStream() throws Exception {
    final HelloServiceClient helloServiceClient = new ClientBuilder<>(HelloServiceClient.class).
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH)).
            build();

    final List<String> names = new ArrayList<>(5);
    final Observable<Response> haim = helloServiceClient.getMessages("haim", 5, false);

    haim.toBlocking().forEach(new Action1<Response>() {
      @Override
      public void call(final Response response) {
        try {
          names.add(response.getResponseBody());
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

    Assert.assertTrue("first name should contain haim", names.get(0).contains("haim"));
  }

  @Test
  public void testTypedResponse() throws Exception {
    final HelloServiceTypedClient helloServiceClient = new ClientBuilder<>(HelloServiceTypedClient.class).
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH)).
            build();

    final TypedResponse<String> response = helloServiceClient.helloWorld().get();
    Assert.assertEquals("response should be hello world", "hello world", response.getTypedBody());
  }

  @Test
  public void testTypedResponseStream() throws Exception {
    final HelloServiceTypedClient helloServiceClient = new ClientBuilder<>(HelloServiceTypedClient.class).
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH)).
            build();

    final List<String> names = new ArrayList<>(5);
    final Observable<TypedResponse<String>> haim = helloServiceClient.getMessages("haim", 5, false);

    haim.toBlocking().forEach(new Action1<TypedResponse<String>>() {
      @Override
      public void call(final TypedResponse<String> response) {
        try {
          names.add(response.getTypedBody());
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

    Assert.assertTrue("first name should contain haim", names.get(0).contains("haim"));
  }

  @Test
  public void testEmptyJsonResponseBody() throws ExecutionException, InterruptedException {
    IHelloService service = createClient(ContentType.JSON, port);
    service.emptyString().get(); // used to throw "JsonMappingException: No content to map due to end-of-input"
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

    for (int i = 0; i < 100; i++) {
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
  public void testStreamJson() {
    testStream(ContentType.JSON);
  }

  @Test
  public void testStreamMsgPack() {
    testStream(ContentType.MESSAGE_PACK);
  }

  private void testStream(final ContentType contentType) {
    final IHelloService client = createClient(contentType, port);
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
      Assert.assertTrue("Got an unexpected exception: " + e.getMessage(), e.getMessage().contains("last message is really bad"));
    }

    Assert.assertEquals(badResults.size(), SIZE);
    Assert.assertEquals(badResults.get(0), "hello moshe #0");
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
      client.hello("moshe").get();
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
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH)).
            build();
  }

  private IFilteredService createFilteredClient(final int port) {
    return new ClientBuilder<>(IFilteredService.class).
            setProtocol(ContentType.JSON).
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + FILTERED_SERVICE_PATH)).
            bindEndpoint("getNextCode", "/next").
            bindEndpoint("getRandomCode", "/random", createCachingFilter()).
            build();
  }

  private IHelloService createClient(final int port, final int requestTimeout, final int retries) {
    return new ClientBuilder<>(IHelloService.class).
            setProtocol(ContentType.JSON).
            setRequestTimeout(requestTimeout).
            setRetries(retries).
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH)).
            build();
  }

  private IHelloService createClientWithFilters(final ContentType protocol, final int port) {
    return new ClientBuilder<>(IHelloService.class).
            setProtocol(protocol).
            addFilter(new BangFilter()).
            bindEndpoint("helloFilter", new QFilter()).
            setRequestTimeout(120000). // heavily loaded testing environment.
            setTargetProvider(new SimpleTargetProvider("http://localhost:" + port + CTX_PATH + HELLO_SERVICE_PATH)).
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

  @Test(expected=ExecutionException.class)
  public void testEmptyTargetBehavior() throws ExecutionException, InterruptedException {
    IHelloService service = new ClientBuilder<>(IHelloService.class).build();
    final ComposableFuture<com.outbrain.ob1k.Response> future =  service.emptyString(); // used to throw "JsonMappingException: No content to map due to end-of-input"
    Assert.assertNotNull(future);
    try {
      future.get();
    } catch (ExecutionException e) {
      Assert.assertEquals(NoSuchElementException.class,e.getCause().getClass());
      throw e;
    }
  }

  @Test(expected=RuntimeException.class)
  public void testEmptyTargetStreamBehavior() throws ExecutionException, InterruptedException {
    IHelloService service = new ClientBuilder<>(IHelloService.class).build();
    final Observable<String> observable =  service.getMessages("name", 1, false);
    Assert.assertNotNull(observable);
    observable.toBlocking().first();
  }

}
