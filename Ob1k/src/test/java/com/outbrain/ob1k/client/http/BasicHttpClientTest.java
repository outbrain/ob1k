package com.outbrain.ob1k.client.http;

import com.ning.http.client.Response;
import com.ning.http.util.Base64;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.concurrent.handlers.*;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.build.AddRawServicePhase;
import com.outbrain.ob1k.server.build.ChoosePortPhase;
import com.outbrain.ob1k.server.build.ExtraParamsPhase;
import com.outbrain.ob1k.server.build.ExtraParamsProvider;
import com.outbrain.ob1k.server.build.PortsProvider;
import com.outbrain.ob1k.server.build.RawServiceProvider;
import com.outbrain.ob1k.server.build.ServerBuilder;
import com.outbrain.ob1k.server.build.StaticResourcesPhase;
import com.outbrain.ob1k.server.build.StaticResourcesProvider;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.msgpack.annotation.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.outbrain.ob1k.concurrent.ComposableFutures.all;
import static com.outbrain.ob1k.concurrent.ComposableFutures.from;
import static com.outbrain.ob1k.concurrent.ComposableFutures.schedule;
import static com.outbrain.ob1k.client.http.HttpClient.param;

/**
 * User: aronen
 * Date: 6/18/13
 * Time: 3:02 PM
 */
public class BasicHttpClientTest {

  public static final java.lang.String HELLO_SERVICE_PATH = "/ello";

  private static int port;
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
    port = address.getPort();
  }

  @AfterClass
  public static void tearDown() {
    server.stop();
  }

  @Test
  public void testNoContent_withJsonPayload() throws InterruptedException, ExecutionException {
    final HttpClient client = new HttpClient();
    final ComposableFuture<Object> f1 = client.httpGet("http://localhost:" + port + "/ello/noJsonContent", Object.class);

    final Object response = f1.get();
    Assert.assertNull("Response should be null", response);
  }

  @Test
  public void testNoContent_withMsgPackPayload() throws InterruptedException, ExecutionException {
    final HttpClient client = new HttpClient();
    final ComposableFuture<Object> f1 = client.httpPost("http://localhost:" + port + "/ello/noMsgPackContent", Msg.class, new Object[0], ContentType.MESSAGE_PACK.responseEncoding());

    final Object response = f1.get();
    Assert.assertNull("Response should be null", response);
  }

  @Test
  public void testHello() throws InterruptedException, ExecutionException {
    final HttpClient client = new HttpClient();
    final ComposableFuture<String> f1 = client.httpGet("http://localhost:" + port + "/ello/hello?name=Ob1k", String.class);

    final String response = f1.get();
    Assert.assertEquals("Unexpected Response", "hello Ob1k", response);
  }

  @Test
  @Ignore
  public void testSimpleGet() throws InterruptedException, ExecutionException {
    final HttpClient client = new HttpClient();
    final ComposableFuture<Response> f1 = client.httpGet("http://www.google.com/search", param("q", "outbrain"));

    final Response response = f1.get();
    Assert.assertEquals(response.getStatusCode(), 200);
  }

  @Test
  @Ignore
  public void testTimeout() throws InterruptedException, ExecutionException {
    final HttpClient client = new HttpClient();
    final ComposableFuture<Response> f1 = client.httpGet("http://www.google.com/search", param("q", "outbrain"));
    final ComposableFuture<Response> f2 = f1.withTimeout(50, TimeUnit.MILLISECONDS);

    try {
      f2.get();
      Assert.fail("should not get response in 100ms");
    } catch (final ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof TimeoutException);
    }
  }

  @Test
  @Ignore
  public void testHAProxyHttpRequest()
  {
    final HttpClient client = new HttpClient(1, 2000, 4000, false, true);
    final String username = "roapi";
    final String password = "$RFVbgt5^YHNmju7";
    final HttpClient.Header authorization = new HttpClient.Header("Authorization", "Basic " + Base64.encode((username + ":" + password).getBytes()));
    final ComposableFuture<Response> resp = client.httpGet("http://lb-30001.chidc1.outbrain.com:81/haproxy?stats;csv;norefresh", authorization);
    resp.onSuccess(new OnSuccessHandler<Response>() {
      @Override
      public void handle(final Response httpResponse) {
        try {
          final String body = httpResponse.getResponseBody();
          System.out.println("body: \n" + body);
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
    });

    try {
      Thread.sleep(4000);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  @Ignore
  public void testComposition() throws InterruptedException, ExecutionException {
//    final HttpClient client = new HttpClient(3, 100, 5000);
    final HttpClient client = new HttpClient();

//    http://dm-10001-prod-nydc1.nydc1.outbrain.com:8080/MissConfiggy/properties?format=json&
//    http://dm-20001-prod-ladc1.ladc1.outbrain.com:8080/MissConfiggy/properties?format=json&

//    ComposableFuture<Response> f1 = client.httpGet("http://dm-10001-prod-nydc1.nydc1.outbrain.com:8080/MissConfiggy/properties?format=json");
//    ComposableFuture<Response> f2 = client.httpGet("http://dm-20001-prod-ladc1.ladc1.outbrain.com:8080/MissConfiggy/properties?format=json");

//    ComposableFuture<List<Response>> all = ComposableFutures.all(true, Arrays.asList(f1, f2));
//    try {
//      List<Response> responses = all.get();
//      for (Response res : responses) {
//        String body = res.getResponseBody();
//        System.out.println("got response");
//      }
//      System.out.println("walla...");
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    } catch (ExecutionException e) {
//      e.printStackTrace();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }


    final ComposableFuture<Response> f1 = client.httpGet("http://www.google.co.il/search", param("q", "outbrain"));
    final ComposableFuture<Response> f2 = client.httpGet("http://www.google.co.il/search", param("q", "waze"));
    final ComposableFuture<Response> f3 = client.httpGet("http://www.google.co.il/search", param("q", "conduit"));
//    ComposableFuture<Response> f1 = client.httpGet("http://wiki.msgpack.org/display/MSGPACK/Home");
//    ComposableFuture<Response> f1 = client.httpGet("http://www.google.co.il/");
//    ComposableFuture<Response> f1 = client.httpGet("http://www.dfdfd.co.il/");

    final ComposableFuture<List<Response>> combined = all(f1, f2, f3);
    final ComposableFuture<Boolean> res = combined.continueOnSuccess(new SuccessHandler<List<Response>, Boolean>() {
      @Override
      public Boolean handle(final List<Response> responses) throws ExecutionException {
        System.out.println("got response in first handler.");
        for (final Response resp : responses) {
          try {
            resp.getResponseBody();
//            System.out.println("---------------------------------------------");
//            System.out.println(body);
//            System.out.println("---------------------------------------------");
          } catch (final IOException e) {
            return false;
          }
        }
        return true;
      }
    });

    combined.onResult(new OnResultHandler<List<Response>>() {
      @Override
      public void handle(final ComposableFuture<List<Response>> result) {
        System.out.println("got result in other handler.");
      }
    });

    final long t1 = System.currentTimeMillis();
    res.get();
    final long t2 = System.currentTimeMillis();

    System.out.println("total time: " + (t2 - t1));

//    ComposableFuture<Response> f2 = f1.continueWith(new FutureResultHandler<Response, Response>() {
//      @Override public ComposableFuture<Response> handle(ComposableFuture<Response> result) throws ExecutionException, InterruptedException {
//        Response response = result.get();
//        int code = response.getStatusCode();
//        if (code == 302) {
//          String location = response.getHeaders("Location").get(0);
//          return client.httpGet(location);
//        } else {
//          return result;
//        }
//      }
//    });
//
//    f2.onResult(new Handler<Response>() {
//      @Override public void handle(ComposableFuture<Response> result) {
//        Response response = null;
//        try {
//          response = result.get();
//          String body = response.getResponseBody();
//          System.out.println("got: body ! " + body);
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        } catch (ExecutionException e) {
//          e.printStackTrace();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    });

//    try {
//      Response r = f2.get();
//      System.out.println("finished.");
//
//      ComposableFuture<Response> f3 = client.httpGet("http://www.google.com/");
//      f3.get();
//
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    } catch (ExecutionException e) {
//      e.printStackTrace();
//    }
  }

  @Test
  @Ignore
  public void testScheduler() {
    final ComposableFuture<String> res1 = calcX();

    final ComposableFuture<String> res2 = res1.continueOnSuccess(new FutureSuccessHandler<String, String>() {
      @Override
      public ComposableFuture<String> handle(final String result) {
        return schedule(new Callable<String>() {
          @Override
          public String call() throws Exception {
            return result + ", phase II";
          }
        }, 10, TimeUnit.MILLISECONDS);
      }
    });

    final ComposableFuture<String> res3 = res2.continueOnSuccess(new FutureSuccessHandler<String, String>() {
      @Override
      public ComposableFuture<String> handle(final String result) {
        return calcY(result);
      }
    });

    try {
      System.out.println(res3.get(100, TimeUnit.SECONDS));
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  @Ignore
  public void testRaniUseCase() {
    final HttpClient client = new HttpClient();
    final String url = "http://kiwi18.leiki.com/focus/api";
    final String body = "method=analyse&apiuser=outbrain&apipassword=oa4Mahsh&target=Champagne&classification=focus100k";
    try {
      final Response response = client.httpPost(url, body, ContentType.X_WWW_FORM_URLENCODED).get();
      final String respBody = response.getResponseBody();

      System.out.println(respBody);
    } catch (InterruptedException | ExecutionException | IOException e) {
      e.printStackTrace();
    }
  }

  private ComposableFuture<String> calcY(final String result) {
    return from(new Callable<String>() {
      @Override
      public String call() throws Exception {
        return result + ", phase III";
      }
    });
  }

  private ComposableFuture<String> calcX() {
    return from(new Callable<String>() {
      @Override
      public String call() throws Exception {
        return "phase I";
      }
    });
  }


  @Message
  public static class Msg {

  }
}
