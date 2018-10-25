package com.outbrain.ob1k.http;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author marenzon
 */
public class ClientBasicFlowsTest {

  private static MockWebServer server;
  private static CustomDispatcher dispatcher;

  private static class CustomDispatcher extends Dispatcher {
    private final BlockingQueue<Function<RecordedRequest, MockResponse>> responseQueue = new LinkedBlockingQueue<>();
    private volatile RecordedRequest lastRequest;
    private final MockResponse failureResponse = new MockResponse().setResponseCode(500).setBody("response queue empty");

    @Override
    public MockResponse dispatch(final RecordedRequest request) throws InterruptedException {
      lastRequest = request;
      if (responseQueue.peek() == null) {
        return failureResponse;
      }
      return responseQueue.take().apply(request);
    }

    public void enqueue(final MockResponse response) {
      enqueue(input -> {
        return response.clone();
      });
    }

    public void enqueue(final Function<RecordedRequest, MockResponse> responseFunction) {
      responseQueue.add(responseFunction);
    }

    public RecordedRequest getRequest() {
      return lastRequest;
    }
  }

  @BeforeClass
  public static void setup() throws IOException {
    server = new MockWebServer();
    dispatcher = new CustomDispatcher();
    server.setDispatcher(dispatcher);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testSimpleRequestResponse() throws Exception {

    final String expected = "hello world";

    dispatcher.enqueue(new MockResponse().setBody(expected));

    final HttpClient httpClient = HttpClient.createDefault();
    final String url = server.url("/helloWorld").toString();
    final Response response = httpClient.get(url).asResponse().get();

    assertEquals("response body should be " + expected, expected, response.getResponseBody());
  }

  @Test
  @Ignore
  public void testStream() throws Exception {

    final String singleResponse = "Hello World";
    final int repeats = 3;
    final String streamBody = Strings.repeat(singleResponse, repeats);

    dispatcher.enqueue(new MockResponse().setChunkedBody(streamBody, streamBody.length() / repeats));

    final HttpClient httpClient = HttpClient.createDefault();
    final String url = server.url("/helloWorldStream").toString();
    final BlockingObservable<Response> responseStream = httpClient.get(url).asStream().toBlocking();

    final AtomicInteger counter = new AtomicInteger(0);
    final AtomicReference<Response> lastResponse = new AtomicReference<>();

    responseStream.forEach(response -> {
      counter.incrementAndGet();
      lastResponse.set(response);
    });

    assertEquals("response elements size equals to repeats", repeats, counter.get());
    assertEquals("chunks contain singleResponse", singleResponse, lastResponse.get().getResponseBody());
  }

  @Test
  public void testRequestWithBody() throws Exception {

    dispatcher.enqueue(request -> {
      return new MockResponse().setBody(request.getBody());
    });

    final String name = "julia";
    final HttpClient httpClient = HttpClient.createDefault();
    final String url = server.url("/getName").toString();
    final Response response = httpClient.post(url).setBody(name).asResponse().get();

    final RecordedRequest request = dispatcher.getRequest();

    assertEquals("response body should be " + name, response.getResponseBody(), name);
    assertEquals("request should be of method post", request.getMethod(), "POST");
  }

  @Test
  public void testRequestWithQueryParam() throws Exception {

    dispatcher.enqueue(request -> {
      // a bit ugly, will changed until MockWebServer will support extracting query params
      return new MockResponse().setBody(request.getPath());
    });

    final HttpClient httpClient = HttpClient.createDefault();
    final String url = server.url("/withQueryParams").toString();
    final String name = "julia";
    final Response response = httpClient.get(url).addQueryParam("name", name).asResponse().get();

    assertTrue("response body should have name " + name, response.getResponseBody().contains("name=" + name));
  }

  @Test
  public void testRequestWithPathParam() throws Exception {

    dispatcher.enqueue(request -> {
      // a bit ugly, will changed until MockWebServer will support extracting query params
      return new MockResponse().setBody(request.getPath());
    });

    final HttpClient httpClient = HttpClient.createDefault();
    final String name = "julia";
    final String url = server.url("/withQueryParams").toString();
    final Response response = httpClient.get(url + "?name={name}").setPathParam("name", name).asResponse().get();

    assertTrue("response body should have name " + name, response.getResponseBody().contains("name=" + name));
  }

  @Test(expected = ExecutionException.class)
  public void testRequestTimeout() throws Exception {

    dispatcher.enqueue(new MockResponse().setBody("hello world").throttleBody(0, 10, TimeUnit.MILLISECONDS));

    final HttpClient httpClient = HttpClient.newBuilder().setRequestTimeout(1).build();
    final String url = server.url("/sleep").toString();
    httpClient.get(url).asResponse().get();

    fail("should have throw ExecutionException - timeout exception");
  }

  @Test(expected = ExecutionException.class)
  public void testResponseMaxSize() throws Exception {

    dispatcher.enqueue(new MockResponse().setBody("Hello World"));

    final HttpClient httpClient = HttpClient.createDefault();
    final String url = server.url("/helloWorld").toString();
    httpClient.get(url).setResponseMaxSize(1).asResponse().get();

    fail("should have throw ExecutionException - response too big exception");
  }

  @Test(expected = RuntimeException.class)
  public void testStreamWithResponseMaxSize() throws Exception {

    dispatcher.enqueue(new MockResponse().setChunkedBody("Hello World", 2));

    final HttpClient httpClient = HttpClient.createDefault();
    final String url = server.url("/stream").toString();
    final Observable<Response> responseObservable = httpClient.get(url).
      setResponseMaxSize(1).
      asStream();

    responseObservable.toBlocking().first();

    fail("should have throw RuntimeException - response size limit is bigger than 1");
  }

  @Test
  public void testBasicAuth() throws Exception {

    dispatcher.enqueue(input -> {
      final String authHeader = input.getHeader("Authorization").replace("Basic ", "");
      final String credentials = new String(Base64.getDecoder().decode((authHeader)));
      return new MockResponse().setBody(credentials);
    });

    final String basicUsername = "moshe";
    final String basicPassword = "junkhead";

    final HttpClient httpClient = HttpClient.createDefault();
    final String url = server.url("/basicAuth").toString();
    final Response response = httpClient.get(url).withBasicAuth(basicUsername, basicPassword).asResponse().get();

    final String basicAuthHeader = basicUsername + ":" + basicPassword;
    assertEquals("response should be '" + basicAuthHeader + "'", basicAuthHeader, response.getResponseBody());
  }
}
