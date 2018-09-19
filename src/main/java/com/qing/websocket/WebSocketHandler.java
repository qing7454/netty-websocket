package com.qing.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
/**
 * websocket 具体业务处理方法
 * 
 * */
public class WebSocketHandler extends ChannelInboundHandlerAdapter{

	private WebSocketServerHandshaker handshaker;
	
	private String userId = "";
		
	/**
	 * 当客户端连接成功，返回个成功信息
	 * */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub	
	}
	
	
	/**
	 * 当客户端断开连接
	 * */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (ChannelsMap.isExist(ctx.channel())) {
			ChannelsMap.remove(userId);
		}		
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// 传统的HTTP接入
		if(msg instanceof FullHttpRequest){			
			handleHttpRequest(ctx,(FullHttpRequest)msg);
			
		}else if(msg instanceof WebSocketFrame){
			// WebSocket接入
			handlerWebSocketFrame(ctx,(WebSocketFrame)msg);
		}						
	}
	
	
	
	public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception{	
		TextWebSocketFrame tw = (TextWebSocketFrame) frame;
		String msg = tw.text();
		//关闭请求
		if(frame instanceof CloseWebSocketFrame){			
			handshaker.close(ctx.channel(), (CloseWebSocketFrame)frame.retain());						
			return;
			
		}
		if (msg.equals("HeartBeat")) {
			System.out.println("Received heart Message -"+userId+""+msg);
			TextWebSocketFrame tws = new TextWebSocketFrame("response heart");
			ctx.channel().writeAndFlush(tws);
		}
		//只支持文本格式，不支持二进制消息
		if(!(frame instanceof TextWebSocketFrame)){			
			throw new Exception("仅支持文本格式");
		}

	}
	
	//第一次请求是http请求，请求头包括ws的信息
	public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req){
		
		//获取用户ID
		userId = findUserIdByUri(req.getUri());
		
		//判断用户权限，通过加入到连接集合
		if (userId != null) {
			boolean idAccess = true; 
			if (idAccess) {
				ChannelsMap.add(userId, ctx.channel());
			}
		}

		if(!req.decoderResult().isSuccess()){
			sendHttpResponse(ctx,req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		
		// 构造握手响应返回
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws:/"+ctx.channel()+ "/websocket",null,false);
		
		handshaker = wsFactory.newHandshaker(req);
		// 请求头不合法, 导致handshaker没创建成功 
		if(handshaker == null){
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		}else{
			//向客户端发送websocket握手,完成握手  
			handshaker.handshake(ctx.channel(), req);
		}
		
	}
	
	public static void sendHttpResponse(ChannelHandlerContext ctx,FullHttpRequest req,DefaultFullHttpResponse res){		
        // 返回应答给客户端
        if (res.status().code() != 200)
        {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.status().code() != 200)
        {
            f.addListener(ChannelFutureListener.CLOSE);
        }		
	}	
    private static boolean isKeepAlive(FullHttpRequest req){
        return false;
    }
   
    //异常处理，netty默认是关闭channel
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		//输出日志
		 cause.printStackTrace();
		 ctx.close();
	}
	
	// 通过Uri获取用户Id uri中包含userId
	private String findUserIdByUri(String uri) {
        String userId = "";
        try {
            userId = uri.substring(uri.indexOf("userId") + 7);
            //trim()去掉字符序列左边和右边的空格
            if (userId != null && userId.trim() != null && userId.trim().length() > 0) {
                userId = userId.trim();
            }
        } catch (Exception e) {
        }
        return userId;
	}
}