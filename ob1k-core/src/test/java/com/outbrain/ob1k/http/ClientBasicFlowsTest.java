package com.outbrain.ob1k.http;

import static org.junit.Assert.*;

import com.outbrain.ob1k.client.http.HelloService;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.build.ChoosePortPhase;
import com.outbrain.ob1k.server.build.PortsProvider;
import com.outbrain.ob1k.server.build.RawServiceProvider;
import com.outbrain.ob1k.server.build.ServerBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.observables.BlockingObservable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author marenzon
 */
public class ClientBasicFlowsTest {

  public static final String HELLO_SERVICE_PATH = "/hello";

  private static String serviceUrl;
  private static Server server;

  @BeforeClass
  public static void setup() {
    server = ServerBuilder.newBuilder().
            configurePorts(new PortsProvider() {
              @Override
              public void configure(final ChoosePortPhase builder) {
                builder.useRandomPort();
              }
            }).
            setContextPath("/").
            withServices(new RawServiceProvider() {

              @Override
              public void addServices(final AddRawServicePhase builder) {
                builder.addService(new HelloService(), HELLO_SERVICE_PATH);
              }
            }).
            build();

    final InetSocketAddress address = server.start();
    serviceUrl = "http://localhost:" + address.getPort() + HELLO_SERVICE_PATH;
  }

  @AfterClass
  public static void tearDown() {
    server.stop();
  }

  @Test
  public void testSimpleRequestResponse() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final Response response = httpClient.get(serviceUrl + "/helloWorld").asResponse().get();

    assertEquals("response body should be \"hello world\"", "\"hello world\"", response.getResponseBody().toLowerCase());
  }

  @Test
  public void testStream() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final int iters = 10;
    final BlockingObservable<Response> responseObservable = httpClient.get(serviceUrl + "/getMessages").
            setBody("[\"haim\", " + iters + ", false]").
            asStream().toBlocking();

    final List<String> names = new ArrayList<>();
    responseObservable.forEach(new Action1<Response>() {
      @Override
      public void call(final Response response) {
        try {
          names.add(response.getResponseBody());
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    });

    assertEquals("response elements size equals to iters", iters, names.size());
    assertTrue("names contains haim", names.get(0).contains("haim"));
  }

  @Test
  public void testRequestWithBody() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final String name = "julia";
    final Response response = httpClient.post(serviceUrl + "/hello").setBody("\"" + name + "\"").asResponse().get();

    assertTrue("response body should contain the word julia", response.getResponseBody().contains(name));
  }

  @Test
  public void testRequestWithQueryParam() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final String name = "julia";
    final Response response = httpClient.get(serviceUrl + "/hello").addQueryParam("name", "\"" + name + "\"").asResponse().get();

    assertTrue("response body should contain the word julia", response.getResponseBody().contains(name));
  }

  @Test
  public void testRequestWithPathParam() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final String name = "julia";
    final Response response = httpClient.get(serviceUrl + "/hello?name={name}").setPathParam("name", "\"" + name + "\"").asResponse().get();

    assertTrue("response body should contain the word julia", response.getResponseBody().contains(name));
  }

  @Test(expected=ExecutionException.class)
  public void testRequestTimeout() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().setConnectionTimeout(1).build();
    httpClient.get(serviceUrl + "/sleep").addQueryParam("milliseconds", "100000").asResponse().get();

    fail("should have throw ExecutionException - timeout exception");
  }

  @Test(expected=ExecutionException.class)
  public void testResponseMaxSize() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    httpClient.get(serviceUrl + "/helloWorld").setResponseMaxSize(1).asResponse().get();

    fail("should have throw ExecutionException - response too big exception");
  }

  @Test(expected = RuntimeException.class)
  public void testStreamWithResponseMaxSize() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final Observable<Response> responseObservable = httpClient.get(serviceUrl + "/getMessages").
            setBody("[\"haim\", 10, false]").
            setResponseMaxSize(1).
            asStream();

    responseObservable.toBlocking().first();

    fail("should have throw RuntimeException - response size limit is bigger than 1");
  }
}