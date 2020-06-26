package org.niss.server;


import org.junit.Test;
import org.niss.connect.UdpConnection;
import org.niss.message.Message;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServerTest {

    @Test
    public void testStart()throws Exception {

//        ServerSocket serverSocket = new ServerSocket(7072);
//        Socket socket = serverSocket.accept();
//        ObjectOutputStream out= new ObjectOutputStream(socket.getOutputStream());
//        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//        System.out.println(in);
//        System.out.println(out);
//        Message message = Message.newMessage(Message.MessageType.LOGIN,"倪守顺","hello");
//        out.writeObject(message);
//        System.out.println(in.readObject());
    }
}