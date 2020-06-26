package org.niss.client;

import org.apache.log4j.Logger;
import org.niss.client.listener.Listener;
import org.niss.message.Message;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端连接性测试
 */
public class ClientTest {

    static Logger logger = Logger.getLogger(ClientTest.class);

    public static void main(String[] args) throws Exception {

        ApplicationContext context = new ClassPathXmlApplicationContext("/application-config.xml");
        Client client = context.getBean("client", Client.class);
        client.setUserName("UserA");
        try {

            client.connect();
            InetSocketAddress addr = client.getConnection().getLocalInetSocketAddress();
            logger.info("connect");
            Listener listener = context.getBean("listener",Listener.class);
            logger.info("监听器添加完成");
            client.start(listener);
            Message message = Message.newMessage(Message.MessageType.PUBLIC_TEXT, client.getUserName(), "hello");
            message.setSocketAddress(addr);
            logger.info("开始发送" + message);
            client.sendMessage(message, 10);
            Thread.sleep(1000);

            List<String> list = new ArrayList<>();
            list.add("UserA");

            Message message1 = Message.newMessage(Message.MessageType.USER_LIST,client.getUserName(),"请求用户列表");
            message1.setSocketAddress(addr);
            client.sendMessage(message1,10);

            int i = 0;
            while (true) {
                Message messageL = Message.newMessage(Message.MessageType.PRIVATE_TEXT, client.getUserName(), "你好-" + i+" from A",list);
                messageL.setSocketAddress(addr);
                Thread.sleep(3000);
                client.sendMessage(messageL, 10);
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

}
