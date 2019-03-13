package com.test.nettydemo.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

/**
 * 使用 MessagePack 自定义编码器
 */
public class MsgPackEncoder extends MessageToByteEncoder<Object> {

    private MessagePack msgPack = new MessagePack();

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        byte[] write = msgPack.write(o);
        byteBuf.writeBytes(write);
    }
}
