package com.zbb.lizi.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @author by tiancha
 * @Date 2019/9/29 14:58
 */
public class NettyServer {
    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        //接受新连接线程
        NioEventLoopGroup boss = new NioEventLoopGroup();
        //读取数据线程
        NioEventLoopGroup worker = new NioEventLoopGroup();
        serverBootstrap                                                 //引导类
                .group(boss, worker)                                    //确定线程模型
                .childOption(ChannelOption.SO_KEEPALIVE, true)    //为每条连接设置TCP属性
                .option(ChannelOption.SO_BACKLOG, 1024)           //为服务端channel设置一些属性
                .channel(NioServerSocketChannel.class)                  //确定IO类型
                .childHandler(new ChannelInitializer<NioSocketChannel>() {//定义数据处理逻辑
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                System.out.println(msg);
                            }
                        });
                    }
                })
                .bind(8000)
        ;
    }
}
