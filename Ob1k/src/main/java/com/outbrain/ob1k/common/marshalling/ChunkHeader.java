package com.outbrain.ob1k.common.marshalling;

/**
 * Created by aronen on 6/30/14.
 *
 * programmatic header for each chunk to indicate whether it represent a success or a failure.
 * in case of a failure it must be the last chunk the stream.
 */
public class ChunkHeader {
  public static final String ELEMENT_HEADER = "Chunk-Status: 200\r\n";
  public static final String ERROR_HEADER = "Chunk-Status: 500\r\n";
}
