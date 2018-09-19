
window.onload = function () {

    document.getElementById("state-info").innerHTML = "正在准备连接服务器……";

    //当前已重连次数，超过上限则不再重连，彻底关闭连接
    var curTryNum = 0;
    var maxTryNum = 10;

    var connect = function (url) {
        //连接次数加一
        curTryNum = curTryNum + 1;

        var websocket = null;
        
        if(!window.WebSocket){  
            window.WebSocket = window.MozWebSocket;  
        } 
        if (window.WebSocket) {
            websocket = new WebSocket(url);
            
            websocket.onopen = function (event) {
                //连接成功时将当前已重连次数归零
                curTryNum = 0;

                document.getElementById("state-info").innerHTML = "连接成功";
                console.log("心跳检测启动");
                heartCheck.start();

            };
            websocket.onclose = function (event) {
                if (curTryNum <= maxTryNum) {
                    document.getElementById("state-info").innerHTML = "连接关闭，5秒后重新连接……";
                    // 5秒后重新连接，实际效果：每5秒重连一次，直到连接成功
                    setTimeout(function () {
                        connect(url);
                    }, 5000);
                } else {
                    document.getElementById("state-info").innerHTML = "连接关闭，且已超过最大重连次数，不再重连";
                }


            };

            websocket.onmessage = function(message) {
                // 无论收到什么信息，说明当前连接正常，将心跳检测的计时器重置
            	heartCheck.reset();
            };
            websocket.onerror = function (event) {
                document.getElementById("state-info").innerHTML = "连接出错";

            };
            
        }else {
            alert("你的浏览器不支持websocket协议");
            window.close();
        }

        //监听窗口关闭事件，窗口关闭前，主动关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常
        window.onbeforeunload = function () {
            websocket.close();
        };
        /**
         * 心跳检测
         */
        var heartCheck = {
            timeout: 5000, //计时器设定为5s
            timeoutObj: null,
            serverTimeoutObj: null,
            //重置
            reset: function() {
                clearTimeout(this.timeoutObj);
                clearTimeout(this.serverTimeoutObj);
                this.start();
            },
            //开始
            start: function() {
                var self = this;
                this.timeoutObj = setTimeout(function() {
                	websocket.send("HeartBeat");
                	console.log("发送心跳");
                }, this.timeout);
            }
        };

    };
    
    var userId  = "12";
    
    /**
     * 执行入口
     */
    var url = "ws://127.0.0.1:8899/?userId=" + userId;
    connect(url);
};


