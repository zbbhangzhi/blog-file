package com.zbb.lizi.netty;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author by tiancha
 * @Date 2019/9/29 14:37
 */
public class IOServer {
    public static void main(String[] args) throws Exception {

        final ServerSocket serverSocket = new ServerSocket(8000);

        // (1) 接收新连接线程
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        // (1) 阻塞方法获取新的连接
                        final Socket socket = serverSocket.accept();

                        // (2) 每一个新的连接都创建一个线程，负责读取数据
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    int len;
                                    byte[] data = new byte[1024];
                                    InputStream inputStream = socket.getInputStream();
                                    // (3) 按字节流方式读取数据
                                    while ((len = inputStream.read(data)) != -1) {
                                        System.out.println(new String(data, 0, len));
                                    }
                                } catch (IOException e) {
                                }
                            }
                        }).start();
                    } catch (IOException e) {
                    }
                }
            }
        }).start();
    }
}
