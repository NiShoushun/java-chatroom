package org.niss.server.listener;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.niss.connect.Connection;
import org.niss.message.Message;
import org.niss.server.TcpServer;
import org.niss.connect.TcpConnection;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP消息监听服务
 * 包含客户端name与相应的TCP连接
 * 用于处理单个客户端TCP连接服务
 *
 * @author Ni187
 */

public class TcpClientListener implements Runnable, AutoCloseable {

    private final TcpConnection connection;
    private final String username;
    private final TcpServer tcpServer;

    Logger logger = LogManager.getLogger(TcpClientListener.class);

    public TcpClientListener(TcpConnection connection, String username, TcpServer tcpServer) {
        this.connection = connection;
        this.username = username;
        this.tcpServer = tcpServer;
    }

    /**
     * 启动监听服务
     */
    @Override
    public void run() {
        try {
            // 循环获取客户端的消息对象
            while (true) {
                try {
                    Message message = connection.receiveObject(Message.class);
                    handleMessage(message);
                } catch (Exception e) {
                    logger.warn(e);
                    break;
                }
            }
        } finally {
            // 通知服务端，转发至所有的客户端，本用户以下线
            logger.info("将" + username + "移除用户列表");
            tcpServer.removeClientConnection(username);
            tcpServer.notifyUserStatus(username, false);
            close();
        }
    }

    /**
     * 用于接受并转发用户发送的公聊/私聊消息
     *
     * @param message 消息对象
     */
    private void handleMessage(Message message) throws IOException {
        InetSocketAddress address = connection.getRemoteInetSocketAddress();
        switch (message.getType()) {
            case PUBLIC_TEXT: {
                logger.info(address + " 发送【公开】消息:\"" + message.getText() + "\"");
                forwardMessage(message);
                logger.info("成功转发" + address + " 的公开消息:\"" + message.getText() + "\"");
                break;
            }
            case PRIVATE_TEXT: {
                logger.info(address + " 发送【私人】消息:\"" + message.getText() + "\"");
                forwardPrivateMessage(message);
                logger.info("成功转发" + address + " 的私人消息:\"" + message.getText() + "\"");
                break;
            }
            case USER_LIST: {
                try {
                    logger.debug(connection.getInetAddress() + "请求用户列表");
                    sendUserList(address);
                    logger.debug("用户列表==>" + connection.getInetAddress() + "已推送");
                } catch (IOException e) {
                    logger.debug("推送用户列表==>" + connection.getInetAddress() + " 失败" + e);
                }
                break;
            }
            default:
        }
    }

    /**
     * 发送私聊文本消息
     *
     * @param message 私人文本消息
     */
    private void forwardPrivateMessage(Message message) {
        InetAddress address = connection.getInetAddress();
        // 从私有消息中获取接收者用户列表，再通过服务类的map获取连接，并发送该消息
        for (String toName : message.getUsernames()) {
            try {
                Connection channel = tcpServer.getTcpConnectionByName(toName);
                channel.sendObject(message);
            } catch (IOException e) {
                logger.warn(address + " 向" + username + "发送私人消息失败: " + e);
            }
        }
    }

    /**
     * 发送群聊文本消息
     *
     * @param message 公开文本消息
     */
    private void forwardMessage(Message message) throws IOException {
        for (TcpConnection tcpConnection : tcpServer.getTcpConnections()) {
            try {
                Message to = Message.newMessage(Message.MessageType.PUBLIC_TEXT, username, message.getText());
                tcpConnection.sendObject(to);
            } catch (Exception e) {
                logger.warn("转发消息" + message + "==> " + connection.getRemoteInetSocketAddress() + " 失败: " + e);
            }
        }
    }

    /**
     * 向用户发送用户列表
     */
    private void sendUserList(InetSocketAddress address) throws IOException {
        ConcurrentHashMap<String, InetSocketAddress> userMap = new ConcurrentHashMap<>();
        for (String username : tcpServer.getUsers()) {
            userMap.put(username, null);
        }
        Message message = Message.newUserListMessage("system", userMap);
        connection.sendObject(message);
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException ioException) {
                logger.debug("关闭客户端监听器发生异常", ioException);
            }
        }
    }
}