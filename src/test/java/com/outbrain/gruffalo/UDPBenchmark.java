package com.outbrain.gruffalo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;

public class UDPBenchmark {

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
    @Override
    public void run() {
      DatagramSocket socket;
      InetAddress localhost;
      try {
        socket = new DatagramSocket();
        localhost = InetAddress.getByName("localhost");
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }

      final StringBuilder payload = new StringBuilder(40);
      final StopWatch time = new StopWatch();
      time.start();
      for (int i = 0; i < 2000000; i++) {
        payload.append(i).append(" - ").append(new DateTime()).append('\n');
        try {
          socket.send(new DatagramPacket(payload.toString().getBytes(), payload.length(), localhost, 2004));
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }

        payload.delete(0, 40);
        //      if (i % 1000 == 0) {
        //        Thread.sleep(100);
        //      }
      }

      time.stop();
      System.out.println("Total time " + time);
    }
  }
}
