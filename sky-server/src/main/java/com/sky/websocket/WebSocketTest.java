package com.sky.websocket;

import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketTest {
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 通过WebSocket每隔10秒向客户端发送测试消息
     */
//    @Scheduled(cron = "0/10 * * * * ?")
    public void sendMessageToClient() {
//        webSocketServer.sendToAllClient("这是来自服务端的测试消息：" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()));

        // 通过WebSocket服务器给管理员端发送来单通知
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1); // 1表示来单通知 2表示客户催单
        map.put("orderId", 1234567890L);
        map.put("content", "订单号：" + 1234567890L);
        String message = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(message);
    }
}
