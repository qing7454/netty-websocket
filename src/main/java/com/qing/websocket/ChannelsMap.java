package com.qing.websocket;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Hashtable;
import java.util.Map;

public class ChannelsMap {
    private static Map<String, Channel> channelMap=new Hashtable<String, Channel>();
    
    public static final void add(String userId,Channel ch){
    	if(channelMap.containsKey(userId)){
    		Channel c=channelMap.get(userId);
    		c.close();
    	}
    	
    	channelMap.put(userId, ch);
    	System.out.println("链接个数："+channelMap.size());
    }
    
    public static Channel get(String userId){
    	return channelMap.get(userId);
    }
    
    public static boolean isExist(Channel ch){
    	return channelMap.containsValue(ch);
    }
    
    public static void remove(String userId){
    	if (channelMap.containsKey(userId)) {
    		channelMap.remove(userId);
		}    	
    }
    
    public static ChannelGroup addToGroup(){
    	ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);  
    	for (String key : channelMap.keySet()) {
			group.add(channelMap.get(key));
		}
    	return group;

    }
}
