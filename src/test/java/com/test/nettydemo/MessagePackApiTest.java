package com.test.nettydemo;

import com.test.nettydemo.decoder.MsgPackDecoder;
import com.test.nettydemo.encoder.MsgPackEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessagePackApiTest {

    /**
     * 使用 MessagePack 进行序列化和反序列化
     *
     * @throws IOException
     */
    @Test
    public void testApi() throws IOException {
        List<String> data = new ArrayList<>();
        data.add("AAAAAA");
        data.add("BBBBBB");
        data.add("CCCCCC");
        MessagePack messagePack = new MessagePack();

        byte[] write = messagePack.write(data);

        List<String> read = messagePack.read(write, Templates.tList(Templates.TString));
        System.out.println(read);
    }

    @Test
    public void testServer() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(65535, 0, 2,0,2));
                            socketChannel.pipeline().addLast("msgpck decoder",new MsgPackDecoder());
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(2));
                            socketChannel.pipeline().addLast("msgpck encoder",new MsgPackEncoder());
                            socketChannel.pipeline().addLast(new ServerHandler());
                        }
                    });

            ChannelFuture sync = serverBootstrap.bind(8088).sync();
            sync.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    @Test
    public void testClient() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast("msgpck decoder",new MsgPackDecoder());
                            socketChannel.pipeline().addLast("msgpck encoder",new MsgPackEncoder());
                            socketChannel.pipeline().addLast(new ClientHandler());
                        }
                    });
            ChannelFuture sync = bootstrap.connect("127.0.0.1", 8088).sync();
            sync.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
            Response response = (Response) o;
            System.out.println(response);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("[CLIENT] channelActive");
            Request request = new Request();
            request.setId(1001);
            request.setName("AA");
            ctx.writeAndFlush(request);
        }
    }

    private class ServerHandler extends SimpleChannelInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
            System.out.println("[SERVER] channelRead0");
            Request r = (Request) o;
            System.out.println(r);

            Response response = new Response();
            response.setId(r.getId());
            response.setResult("SUCCESS");
            channelHandlerContext.writeAndFlush(response);
        }
    }

    private class Request {
        private int id;

        private String name;

        public int getId() {
            return id;
        }

        public Request setId(int id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Request setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Request{");
            sb.append("id=").append(id);
            sb.append(", name='").append(name).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    private class Response {
        private int id;

        private String result;

        public int getId() {
            return id;
        }

        public Response setId(int id) {
            this.id = id;
            return this;
        }

        public String getResult() {
            return result;
        }

        public Response setResult(String result) {
            this.result = result;
            return this;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Response{");
            sb.append("id=").append(id);
            sb.append(", result='").append(result).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
