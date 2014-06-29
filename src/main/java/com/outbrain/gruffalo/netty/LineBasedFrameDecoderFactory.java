package com.outbrain.gruffalo.netty;

import io.netty.handler.codec.LineBasedFrameDecoder;

public interface LineBasedFrameDecoderFactory {

  public LineBasedFrameDecoder getLineFramer();
}
