package com.qing.websocket;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author User
 *该类在全局当中只能创建一个对象（GameServerInitializer.main中已经创建）,其它对象用该类的静态方法getInstance（）获取该实例即可。
 */
public class NettyWebsocketServer {
	private static   NettyWebsocketServer instance;
	public static final NettyWebsocketServer getInstance(){
		return instance;
	}
	
	private int port;
	
	
	
	public NettyWebsocketServer(int port) {
		if(instance!=null){
			return;
		}
		
		this.port = port;
		try {
			this.start();
			instance=this;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void start() throws Exception{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class); 
			b.childHandler(new myChannelInitializer());
			b.option(ChannelOption.SO_BACKLOG, 128);
			b.childOption(ChannelOption.SO_KEEPALIVE, true);
		
			ChannelFuture f = b.bind(port).sync(); 
			System.out.println("Netty 服务端开始服务.........");
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	
	private class myChannelInitializer extends  ChannelInitializer<SocketChannel>{
		@Override
		public void initChannel(SocketChannel ch) throws Exception {		
			ch.pipeline().addLast("http-codec", new HttpServerCodec());			
			ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));  
			ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
			ch.pipeline().addLast("handler", new WebSocketHandler());
			
		}
	}
}
