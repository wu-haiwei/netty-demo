package com.test.nettydemo.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;

import java.util.List;

/**
 * 使用 MessagePack 自定义解码器
 */
public class MsgPackDecoder extends MessageToMessageDecoder<ByteBuf> {

    private MessagePack msgPack = new MessagePack();

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

        final byte[] array;
        final int leng = byteBuf.readableBytes();
        array = new byte[leng];
        byteBuf.getBytes(byteBuf.readerIndex(), array, 0, leng);
        list.add(msgPack.read(array));
    }
}
