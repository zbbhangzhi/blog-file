package com.zbb.lizi.netty;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 * @author by tiancha
 * @Date 2019/9/29 14:36
 */
public class IOClient {
    public static void main(String[] args) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Socket socket = new Socket("127.0.0.1", 8000);
                    while (true) {
                        try {
                            socket.getOutputStream().write((new Date() + ": hello world").getBytes());
                            Thread.sleep(2000);
                        } catch (Exception e) {
                        }
                    }
                } catch (IOException e) {
                }
            }
        }).start();
    }
}
