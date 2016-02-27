package com.outbrain.ob1k.security.server;

import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.security.common.SecureService;
import com.outbrain.ob1k.security.common.TestClient;
import com.outbrain.ob1k.security.common.TestServer;
import com.outbrain.ob1k.security.common.UnsecureService;
import com.outbrain.ob1k.server.Server;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;

/**
 * Created by gmarom on 6/24/15
 */
public class SecureServerTest {

  @Test
  public void testUnsecureEndpoint() throws Exception {
    Server server = null;
    UnsecureService service = null;
    try {
      server = TestServer.newServer();
      final InetSocketAddress serverAddress = server.start();
      service = TestClient.newClient(serverAddress.getPort(),
                                     UnsecureService.class);
      final String val = "val";
      assertEquals(val, service.returnString(val).get());
    } finally {
      Clients.close(service);

      if (server != null) {
        server.stop();
      }
    }
  }

// todo unignore this once I can create a client that can handle a server returning 401
//  @Test
//  public void testSecureEndpoint() throws Exception {
//    Server server = null;
//    SecureService service = null;
//    try {
//      server = TestServer.newServer();
//      final InetSocketAddress serverAddress = server.start();
//      service = TestClient.newClient(serverAddress.getPort(),
//                                   SecureService.class);
//      final String val = "val";
//      assertEquals(val, service.returnString(val).get());
//    } finally {
//      Clients.close(service);
//
//      if (server != null) {
//        server.stop();
//      }
//    }
//  }

}
