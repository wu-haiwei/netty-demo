package com.test.nettydemo;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * NIO 编程源码
 */
public class NioDemoApplicationTest {

    /**
     * 服务端
     */
    @Test
    public void testNioService() {
        MultiplexerTimerServer server = new MultiplexerTimerServer(8088);
        new Thread(server, "server-1").start();
        new Scanner(System.in).next();
    }

    /**
     * 客户端
     */
    @Test
    public void testNioClient() {
        TimeClientHandler handler = new TimeClientHandler("127.0.0.1", 8088);
        handler.run();
    }

    private class MultiplexerTimerServer implements Runnable {

        private Selector selector;

        private ServerSocketChannel serverSocketChannel;

        private volatile boolean stop;

        public MultiplexerTimerServer(int port) {
            try {
                stop = false;
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                System.out.println("server init.");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    selector.select(1000);
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey nextKey = null;
                    while (iterator.hasNext()) {
                        nextKey = iterator.next();
                        iterator.remove();
                        try {
                            handleInput(nextKey);
                        } catch (Exception e) {
                            if (nextKey != null) {
                                nextKey.cancel();
                                if (nextKey.channel() != null) {
                                    nextKey.channel().close();
                                }
                            }
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleInput(SelectionKey nextKey) throws IOException {
            if (nextKey.isValid()) {
                if (nextKey.isAcceptable()) {
                    ServerSocketChannel ssc = (ServerSocketChannel) nextKey.channel();
                    SocketChannel accept = ssc.accept();
                    accept.configureBlocking(false);
                    accept.register(selector, SelectionKey.OP_READ);
                }
                if (nextKey.isReadable()) {
                    SocketChannel sc = (SocketChannel) nextKey.channel();
                    ByteBuffer bf = ByteBuffer.allocate(1024);
                    int readSize = sc.read(bf);
                    if (readSize > 0) {
                        bf.flip();
                        byte[] data = new byte[bf.remaining()];
                        bf.get(data);
                        String body = new String(data, "UTF-8");
                        System.out.println(body);

                        doWrite(sc, new Date().toString());
                    } else if (readSize < 0) {
                        // tcp链接关闭
                        nextKey.cancel();
                        sc.close();
                    } else {
                        // 没有读取到内容忽略
                    }
                }
            }
        }

        private void doWrite(SocketChannel sc, String data) throws IOException {
            if (data == null || data.trim().length() <= 0) {
                return;
            }
            byte[] bytes = data.getBytes();
            ByteBuffer allocate = ByteBuffer.allocate(bytes.length);
            allocate.put(bytes);
            allocate.flip();
            sc.write(allocate);
        }
    }


    private class TimeClientHandler implements Runnable {

        private String ip;

        private int port;

        private Selector selector;

        private SocketChannel socketChannel;

        private volatile boolean stop;

        public TimeClientHandler(String ip, int port) {
            try {
                this.ip = ip;
                this.port = port;
                this.stop = false;
                this.selector = Selector.open();
                this.socketChannel = SocketChannel.open();
                this.socketChannel.configureBlocking(false);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public void run() {
            try {
                doConnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (!stop) {
                try {
                    selector.select(1000);
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey next = null;
                    while (iterator.hasNext()) {
                        next = iterator.next();
                        iterator.remove();
                        try {
                            handleInput(next);
                        } catch (Exception e) {
                            if (next != null) {
                                next.cancel();
                                if (next.channel() != null) {
                                    next.channel().close();
                                }
                            }
                            e.printStackTrace();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        private void handleInput(SelectionKey next) throws IOException {
            if (next.isValid()) {
                SocketChannel sc = (SocketChannel) next.channel();
                if (next.isConnectable()) {
                    if (sc.finishConnect()) {
                        sc.register(selector, SelectionKey.OP_READ);
                        doWrite(sc);
                    } else {
                        System.exit(1);
                    }
                }
                if (next.isReadable()) {
                    ByteBuffer bf = ByteBuffer.allocate(2014);
                    int readSize = sc.read(bf);
                    if (readSize > 0) {
                        bf.flip();
                        byte[] data = new byte[bf.remaining()];
                        bf.get(data);
                        String result = new String(data, "UTF-8");
                        System.out.println(result);
                        this.stop = true;
                    } else if (readSize < 0) {
                        next.cancel();
                        next.channel().close();
                    }
                }
            }
        }

        private void doConnect() throws IOException {
            boolean connect = socketChannel.connect(new InetSocketAddress(ip, port));
            if (connect) {
                socketChannel.register(selector, SelectionKey.OP_READ);
                doWrite(socketChannel);
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
        }

        private void doWrite(SocketChannel socketChannel) throws IOException {
            byte[] data = "QUERY SERVER TIME".getBytes();
            ByteBuffer bf = ByteBuffer.allocate(data.length);
            bf.put(data);
            bf.flip();
            socketChannel.write(bf);
            if (!bf.hasRemaining()) {
                System.out.println("Send order 2 server succeed.");
            }
        }
    }
}
