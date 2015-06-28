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

import java.net.InetSocketAddress;
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
    final Response response = httpClient.get(serviceUrl + "/helloWorld").execute().get();

    assertEquals("response body should be \"hello world\"", "\"hello world\"", response.getResponseBody().toLowerCase());
  }

  @Test
  public void testRequestWithBody() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final String name = "julia";
    final Response response = httpClient.post(serviceUrl + "/hello").setBody("\"" + name + "\"").execute().get();

    assertTrue("response body should contain the word julia", response.getResponseBody().contains(name));
  }

  @Test
  public void testRequestWithQueryParam() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final String name = "julia";
    final Response response = httpClient.get(serviceUrl + "/hello").addQueryParam("name", "\"" + name + "\"").execute().get();

    assertTrue("response body should contain the word julia", response.getResponseBody().contains(name));
  }

  @Test
  public void testRequestWithPathParam() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().build();
    final String name = "julia";
    final Response response = httpClient.get(serviceUrl + "/hello?name={name}").setPathParam("name", "\"" + name + "\"").execute().get();

    assertTrue("response body should contain the word julia", response.getResponseBody().contains(name));
  }

  @Test(expected=ExecutionException.class)
  public void testRequestTimeout() throws Exception {

    final HttpClient httpClient = HttpClient.newBuilder().setConnectionTimeout(1).build();
    httpClient.get(serviceUrl + "/sleep").addQueryParam("milliseconds", "100000").execute().get();

    fail("should have throw ExecutionException");
  }
}