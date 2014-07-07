package com.outbrain.gruffalo.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * Decodes a UDP packet to it's {@link String} payload.
 * @author eran
 */
public class DatagramPacketToStringDecoder extends MessageToMessageDecoder<DatagramPacket> {

  @Override
  protected void decode(ChannelHandlerContext channelHandlerContext, DatagramPacket msg, List<Object> out) throws Exception {
    out.add(msg.content().toString(CharsetUtil.UTF_8));
  }
}
