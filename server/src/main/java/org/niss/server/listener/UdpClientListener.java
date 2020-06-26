package org.niss.server.listener;

import org.apache.log4j.Logger;
import org.niss.connect.UdpConnection;
import org.niss.message.Message;
import org.niss.server.TcpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 * UDP消息监听服务，用于接受来自客户端的UDP消息，并处理
 *
 * @author Ni187
 */
public class UdpClientListener implements Runnable, AutoCloseable {

    public UdpConnection udpConnection;

    private final TcpServer server;

    public boolean closed = false;

    Logger logger = Logger.getLogger(UdpClientListener.class);

    public UdpClientListener(UdpConnection udpConnection, TcpServer server) {
        this.udpConnection = udpConnection;
        this.server = server;
    }

    @Override
    public void run() {
        while (!closed) {
            try {
                Message message = udpConnection.receiveMessage();
                logger.info(message);
                // 利用server的线程池处理转发消息
                server.getExec().execute(()->{
                    try {
                        handleMessage(message);
                    }catch (IOException e){
                        logger.debug("处理"+message+"时发生异常");
                    }
                });
            } catch (IOException | ClassNotFoundException e) {
                logger.debug("接收到一个异常数据" + e);
            }
        }
    }

    /**
     * 用于接受并转发用户发送的公聊/私聊消息
     *
     * @param message 消息对象
     */
    private void handleMessage(Message message) throws IOException {
        switch (message.getType()) {
            case PUBLIC_TEXT: {
                logger.info(message.getSocketAddress() + " 发送【公开】消息:\"" + message.getText() + "\"");
                // 遍历所有的InetSocketAddress,转发消息
                for(InetSocketAddress address : server.getAllClientInetSocketAddr()) {
                    forwardMessage(message, address);
                    logger.info("成功转发" + address + " 的公开消息:\"" + message.getText() + "\"");
                }
                break;
            }
            case PRIVATE_TEXT: {
                // 从消息中获取所有的待接收的用户名
                for(String username: message.getUsernames()) {
                    // 获取接收方的udp套接字
                    InetSocketAddress address = server.getClientInetSocketAddr(username);
                    forwardMessage(message,address);
                    logger.info("成功转发来自" + address + " 的私人消息:\"" + message + "\"");
                }
                break;
            }
            case USER_LIST: {
                // 获取发送者的套接字
                InetSocketAddress address = server.getClientInetSocketAddr(message.getSenderName());
                try {
                    logger.debug(address+" 请求用户列表");
                    // 发送用户列表至请求客户端
                    sendUserList(address);
                    logger.debug("用户列表==>" + address+ "已推送");
                }catch (IOException e) {
                    logger.debug("推送用户列表==>" + address + " 失败" + e);
                }
                break;
            }
            default:break;
        }
    }


    /**
     * 将接受到的Message对象转发message对象的目的主机
     * @param message 消息对象
     * @throws IOException 数据传输异常
     */
    private void forwardMessage(Message message, InetSocketAddress socketAddress)throws IOException {
        if(message==null||socketAddress==null){
            logger.debug("转发异常，未知套接字："+socketAddress+", message："+message);
            return ;
        }
        udpConnection.sendObject(message, socketAddress);
    }

    /**
     * 计算获取并发送用户列表
     * @param socketAddress 接收方地址
     * @throws IOException 数据传送异常
     */
    private void sendUserList(InetSocketAddress socketAddress) throws IOException {
        ConcurrentHashMap<String,InetSocketAddress> concurrentHashMap = server.getUsernameInetAddrMap();
        Message message = Message.newUserListMessage("system",concurrentHashMap);
        // 重新设置为服务器的套接字
        message.setSocketAddress(udpConnection.getLocalInetSocketAddress());
        forwardMessage(message, socketAddress);
    }


    @Override
    public void close() {
        this.udpConnection.close();
    }

}
