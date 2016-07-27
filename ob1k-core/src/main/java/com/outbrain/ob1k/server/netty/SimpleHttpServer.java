package com.outbrain.ob1k.server.netty;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by aronen on 6/10/14.
 * <p>
 * creates a chunked encoding response.
 */
public class SimpleHttpServer {
  public static boolean isOpen = true;

  public static void main(final String[] args) throws IOException {
    ServerSocketChannel socket = ServerSocketChannel.open();
    socket = socket.bind(new InetSocketAddress(8080));

    while (isOpen) {
      final SocketChannel requestSocket = socket.accept();
      new Thread(() -> {
        try {
          handleRequest(requestSocket);
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }).start();

    }
  }

  private static void handleRequest(final SocketChannel channel) throws Exception {
//        requestSocket.setKeepAlive(true);
//    requestSocket.setSoLinger(false, 0);
//        requestSocket.setTcpNoDelay(true);

    channel.configureBlocking(true);

    final ByteBuffer requestBuff = ByteBuffer.allocate(1024);
    final int bytesRead = channel.read(requestBuff);
    if (bytesRead <= 0) {
      System.out.println("empty request. aborting on socket: " + channel);
      channel.close();
      return;
    } else {
      System.out.println("accepting request on socket: " + channel);
    }

    final String request = new String(requestBuff.array(), 0, bytesRead, "UTF8");
    System.out.println(request);

//        final Writer writer = new OutputStreamWriter(outStream);
    write(channel, "HTTP/1.1 200 OK");
    newLine(channel);

//    final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault());
//    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//    write(channel, "Date: " + dateFormat.format(new Date()));
//    newLine(channel);

    write(channel, "Content-Type: application/json");
//    write(channel, "Content-Type: text/html");
//    write(channel, "Content-Type: text/plain");
    newLine(channel);

//    write(channel, "Server: Apache/2.4.9 (Unix)");
//    newLine(channel);

    write(channel, "Connection: Keep-Alive");
    newLine(channel);

    if (request.contains("favicon")) {
      newLine(channel);
      channel.close();
      return;
    }

    write(channel, "Transfer-Encoding: chunked");
    newLine(channel);

    // end of headers
    newLine(channel);

//    writeChunk(channel, "<html><body>");
//    writeChunk(channel, "[");

    for (int i = 0; i < 200; i++) {
//      final String message = "<div>Hello with a longer message ..... and some text as well ......." + i + "</div>";
//      final String message = "Hello with a longer message ..... and some text as well ......." + i + "<br />";
      final String message = "Hello with a longer message ..... and some text as well ......." + i + "\n";
//      final String message = "{ \"message\": \"Hello with a longer message .....\", \"extra\": \"and some text as well ......." + i + "\" }" + ((i < 199) ? "," : "");
      writeChunk(channel, message);

      Thread.sleep(100);
    }

//    writeChunk(channel, "</body></html>");
//    writeChunk(channel, "]");

    // closing packet.
    write(channel, "0");
    newLine(channel);
    newLine(channel);

    channel.close();
  }

  private static void writeChunk(final SocketChannel channel, final String message) throws IOException {
    write(channel, Integer.toHexString(message.length()));
    newLine(channel);
    write(channel, message);
    newLine(channel);
  }

  private static void write(final SocketChannel channel, final String text) throws IOException {
    channel.write(ByteBuffer.wrap(text.getBytes("UTF8")));
  }

  private static void newLine(final SocketChannel channel) throws IOException {
    write(channel, "\r\n");
  }
}
