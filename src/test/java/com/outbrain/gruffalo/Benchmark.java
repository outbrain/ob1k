package com.outbrain.gruffalo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;

public class Benchmark {

  /**
   * @param args
   * @throws IOException 
   * @throws InterruptedException 
   */
  public static void main(final String[] args) throws IOException, InterruptedException {
    final int nThreads = 8;
    final ExecutorService executor = Executors.newFixedThreadPool(nThreads);

    for (int i = 0; i < nThreads; i++) {
      executor.execute(new BenchmarkTask());
    }

    executor.awaitTermination(60, TimeUnit.SECONDS);
    executor.shutdown();
  }

  private static class BenchmarkTask implements Runnable {

    private Socket socket;
    private BufferedWriter writer;

    @Override
    public void run() {
      connect();

      final StopWatch time = new StopWatch();
      time.start();
      try {
        for (int i = 0; i < 20000000; i++) {
          final StringBuilder payload = new StringBuilder(40);
          payload.append(i).append(" - ").append(new DateTime()).append('\n');
          try {
            writer.write(payload.toString());
            if (i % 10000 == 0) {
              writer.flush();
            }
            if (i % 250000 == 0) {
              Thread.sleep(300);
            }
            if (i % 500000 == 0) {
              System.out.println("sent " + i);
//              Thread.sleep(200);
//              disconnect();
//              connect();
            }
          } catch (final IOException e) {
            throw new RuntimeException(e);
          } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return;
          }

//          payload.delete(0, 40);
        }

        try {
          writer.flush();
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      } finally {
        disconnect();
      }
      time.stop();
      System.out.println("Total time " + time);
    }

    private void connect() {
      try {
        socket = new Socket("localhost", 3003);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void disconnect() {
      try {
        writer.close();
        socket.close();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
