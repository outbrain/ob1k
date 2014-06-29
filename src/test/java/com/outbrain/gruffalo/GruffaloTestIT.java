package com.outbrain.gruffalo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.outbrain.globals.util.STProperties;
import com.outbrain.ob1k.server.AbstractOb1kServerTestBase;
import com.outbrain.ob1k.server.Server;

public class GruffaloTestIT extends AbstractOb1kServerTestBase {

  private static final Logger log = LoggerFactory.getLogger(GruffaloTestIT.class);

  @Test
  public void testSelfTest() throws Exception {
    assertSelfTestOk();
  }

  @Test
  public void testTcpServer() throws Exception {
    final String payload = createPayload();

    final Socket socket = new Socket("localhost", STProperties.getInstance().getInt("com.outbrain.gruffalo.tcp.port"));
    final Writer writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    for (int i = 0; i < 100; i++) {
      writer.write(payload);
    }

    try {
      writer.close();
      socket.close();
    } catch (IOException e) {
      log.error("Failed to close writer / socket", e);
    }
  }

  private String createPayload() {
    final char[] payload = new char[1024];
    Arrays.fill(payload, 'x');
    payload[payload.length - 1] = '\n';
    return new String(payload);
  }

  @Override
  protected Server buildServer() {
    return new GruffaloServer().build(false);
  }
}
