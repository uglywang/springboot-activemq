# SpringBoot-ActiveMq
一个简单的整合案例, 初步实现 `activemq` 的 `queue` 队列模式 和 `topic` 订阅模式

## 准备工作
- springboot2.0.3
- 如果要使用 `activemq pool` 需要另外加入 `activemq-pool` 包

## 简介
`spring-jms-*.RELEASE.jar` 有提供 `JmsMessagingTemplate` 给我们使用, 无特殊要求,我们直接拿来用即可.

## queue模式 与 topic模式
`**queue模式**` `点对点的发送信息`, 点对点通信，每个消息只有一个消费者，消息保证送达，离线消费者可以在下次上线后收到之前积压的消息。
`案例: ` 如果生产者产生了100条消息，那么两个消费同时在的话，会分工合作来接收这100条消息。就是每个消费者接收到50条来处理。     
    
`**topic模式**` `广播形式(主题模式)` 当前有几个客户端在线，就发送几条广播给客户端。 `案例` 如果生产者产生了100条消息,消费者在还没有订阅这个主题之前，是不会接收到这100条消息的。
消费者只有在订阅了某个主题消息之后，生产者产生的消息 才会被接收处理。如果又两个消费者同时订阅了这个主题消息，生产者在产生100条消息时，两个消费者会同时分别接收到这100条消息。

## 发送
```java
package com.example.dxp.web;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTempQueue;
import org.apache.activemq.command.ActiveMQTempTopic;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.Destination;

/**
 * @author carzy
 * @date 2018/08/01
 */
@RestController
public class TestController {

    private JmsMessagingTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsMessagingTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @GetMapping("jms/queue")
    public void jmsQueueTemplate(@RequestParam String value) {
        Destination destination = new ActiveMQQueue("queueTest");
        this.jmsTemplate.convertAndSend(destination, value);
    }

    @GetMapping("jms/topic")
    public void jmsTopicTemplate(@RequestParam String value) {
        // 可以将以下步骤封装进service 层, 并暴露出一个 destinationName 和 message 出来
        Destination destination = new ActiveMQTopic("topicTest");
        this.jmsTemplate.convertAndSend(destination, value);
    }

}

```
### 发送 `queue模式` 消息
`JmsMessagingTemplate` 通过 `convertAndSend()` 方法将消息发送至 `activemq` 中,  这个方法其中有一个重载方法需要我们穿入一个 `Destination` 接口对象,而这个 对象就决定的了我们的消息
发送到哪里, 如上代码,我们用的是 `Destination` 的一个实现类 `ActiveMQQueue`, 表示我们的消息将发送到 `queues`下. 代项目启动,发送成功了, 可在浏览器上访问在 `activemq` 服务的 `8161` 端口,
在 `Queues` 也签下可看见对应的 queue name, `queueTest`

### 发送 `广播形式` 消息
与 `queue模式` 类似, 无非是将 `Destination` 的实现类换成 `ActiveMQTopic`, 在浏览器的 `Topics` 也签也可看见名为 `topicTest` 的topic 通道

## 监听
系统默认采用的是 `queue模式` 模式, 所以我们需要做一下设置,来更改默认的配置
```java
package com.example.dxp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;

import javax.jms.ConnectionFactory;

/**
 * @author carzy
 * @date 2018/08/01
 */
@Configuration
@EnableJms
public class JmsConfig {


    @Bean("jmsQueueListenerContainerFactory")
    public JmsListenerContainerFactory jmsQueueListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory =
                new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        //设置连接数
        factory.setConcurrency("3-10");
        //重连间隔时间
        factory.setRecoveryInterval(1000L);
        factory.setPubSubDomain(false);
        return factory;

    }

    @Bean("jmsTopicListenerContainerFactory")
    public JmsListenerContainerFactory jmsTopicListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory =
                new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        //重连间隔时间
        factory.setPubSubDomain(true);
        return factory;

    }

}

```
### 监听 `queue模式` 消息
我们定义了一个自己的 `监听工厂`, 并且设置它的  `factory.setPubSubDomain(false)`, 表示它是queue模式下的监听工厂.然后我们在使用 `@JmsListener` 进行监听的时候,设置上 这个工厂即可
```java
package com.example.dxp.jms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
/**
 * @author carzy
 * @date 2018/08/01
 */
@Component
public class QueueReceiver {

    @JmsListener(destination = "queueTest", containerFactory = "jmsQueueListenerContainerFactory")
    public void receive(String msg) {
        System.out.println("queue1 监听到的消息内容为: " + msg);
    }

    @JmsListener(destination = "queueTest", containerFactory = "jmsQueueListenerContainerFactory")
    public void receive2(String msg) {
        System.out.println("queue2 监听到的消息内容为: " + msg);
    }
}

```
### 监听 `广播形式` 消息
与上面相反的地方就是 `factory.setPubSubDomain(true)` 表示它是一个 topic模式下的监听工厂,在监听的时候,只需要将 `@JmsListener` 的 `containerFactory` 指向它即可
```java
package com.example.dxp.jms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
/**
 * @author carzy
 * @date 2018/08/01
 */
@Component
public class TopicReceiver {

    @JmsListener(destination = "topicTest", containerFactory = "jmsTopicListenerContainerFactory")
    public void receive(String msg) {
        System.out.println("topicTest1 监听到的消息内容为: " + msg);
    }

    @JmsListener(destination = "topicTest", containerFactory = "jmsTopicListenerContainerFactory")
    public void receive2(String msg) {
        System.out.println("topicTest2 监听到的消息内容为: " + msg);
    }
}

```

## 测试
可通过浏览器访问 `jms/queue?value=test` 和 `jms/topic?value=test` 来出发消息的发送,来观察控制台获取信息的情况