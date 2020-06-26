package org.niss.client;

import org.niss.client.listener.Listener;
import org.niss.client.listener.Listeners;
import org.niss.connect.UdpConnection;
import org.niss.message.Message;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class UdpClientTest {
    public static void main(String[] args) throws Exception{

        ApplicationContext context = new ClassPathXmlApplicationContext("/application-config.xml");
        Listener listener = context.getBean("listener",Listener.class);
        Client client = context.getBean("client", Client.class);
        client.setUserName("UserA");
        client.connect();
        client.start(listener);


    }
}
