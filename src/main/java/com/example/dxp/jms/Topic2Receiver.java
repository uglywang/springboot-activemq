package com.example.dxp.jms;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * 可监听历史消息
 *
 * @author carzy
 * @date 2018/08/01
 */
@Component
public class Topic2Receiver {

    @JmsListener(destination = "topicTest", containerFactory = "jmsTopicListenerContainerFactory2")
    public void receive(String msg) {
        System.out.println("这是持久订阅: " + msg);
    }

}
