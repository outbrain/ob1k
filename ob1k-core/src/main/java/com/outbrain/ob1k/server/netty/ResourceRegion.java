package com.outbrain.ob1k.server.netty;


import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * User: aronen
 * Date: 8/8/13
 * Time: 7:22 PM
 */
public class ResourceRegion extends AbstractReferenceCounted implements FileRegion {
  private static final Logger logger = LoggerFactory.getLogger(ResourceRegion.class);
  private static final int BUFFER_SIZE = 64 * 1024; //64Kb

  private final InputStream stream;
  private final byte[] buff;
  private final long count;
  private ByteBuffer tempBuffer;
  private long transfered = 0;


  public ResourceRegion(final InputStream stream, final long count) {
    this.stream = stream;
    this.buff = new byte[BUFFER_SIZE];
    this.count = count;
  }

  @Override
  public long position() {
    return 0;
  }

  @Override
  public long count() {
    return count;
  }

  @Override
  protected void deallocate() {
    try {
      stream.close();
    } catch (final IOException e) {
      logger.warn("error closing resource", e);
    }
  }

  @Override
  public long transfered() {
    return transfered;
  }

  @Override
  public long transferTo(final WritableByteChannel target, final long position) throws IOException {
    final ByteBuffer buffer;
    if (tempBuffer != null) {
      buffer = tempBuffer;
    } else {
      final int bytesRead = stream.read(buff);
      if (bytesRead != -1) {
        buffer = ByteBuffer.wrap(buff, 0, bytesRead);
      } else {
        return 0L;
      }
    }

    final int bytesWritten = target.write(buffer);
    if (buffer.hasRemaining()) {
      tempBuffer = buffer;
    } else {
      tempBuffer = null;
    }

    transfered += bytesWritten;
    return bytesWritten;
  }

}
