package com.sky;

import com.alibaba.fastjson2.JSON;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.websocket.enabled=true")
public class WebSocketTests {

    @Autowired
    WebSocketServer webSocketServer;

    // 不对，不能这么测试，这个webSocketServer 和 主程序中的 不是同一个实例
    @Disabled("Only run manually")
    @Test
    public void webSocketTest() {
        System.out.println("WebSocket test executed");
        Map map = new HashMap<>();
        map.put("type", 1); // 1表示来单通知 2表示客户催单
        map.put("orderId", 1234567890L);
        map.put("content", "订单号：" + 1234567890L);
        String message = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(message);

    }
}
