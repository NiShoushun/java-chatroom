package org.niss.client.listener;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import org.apache.log4j.Logger;
import org.niss.connect.Connection;
import org.niss.message.Message;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author Ni187
 * <p>
 * TCP消息监听服务，用于监听服务端发送的消息
 */

public class ServerListener implements Listener {

    private final Connection connection;

    private ExecutorService executor;

    private final Logger logger = Logger.getLogger(ServerListener.class);

    ReentrantLock lock = new ReentrantLock();

    private String name;

    public List<String> users;

    /**
     * GUI文本消息控件
     */
    private TextInputControl controller;

    public ServerListener(Connection connection, ExecutorService executor) {
        this.connection = connection;
        this.executor = executor;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setUserList(List<String> users) {
        this.users = users;
    }

    /**
     * 设置控制组件
     *
     * @param controller 控制组件
     */
    @Override
    public void setTextArea(TextArea controller) {
        this.controller = controller;
    }

    /**
     * 监听来自服务器的消息并处理消息功能
     */
    @Override
    public void run() {
        // 错误计数
        int errCount = 5;
        while (true) {
            try {
                Message message = connection.receiveObject(Message.class);
                // 线程池处理接受到的消息
                executor.execute(() -> handelMessage(message));
                // 重置为0
                errCount = 0;
            } catch (IOException | ClassNotFoundException e) {
                logger.debug("接受来自服务端的消息失败", e);
                // 如果发生错误连续超过5次直接退出
                if (errCount < 0) {
                    break;
                }
                --errCount;
            }
        }

    }

    /**
     * 处理服务端发送的消息
     *
     * @param message 消息对象
     */
    private void handelMessage(Message message) {
        lock.lock();
        try {
            logger.debug("[" + connection.getRemoteInetSocketAddress() + "] ==> " + message);
            if (message.getSenderName().equals(name)) {
                return;
            }
            switch (message.getType()) {
                // 接收到服务器转发的公聊信息通知
                case PUBLIC_TEXT: {
                    logger.debug("public[" + message.getSenderName() + "]" + "==> " + message.getText());
                    executor.execute(() -> {
                        if (controller != null) {
                            controller.appendText("public " + new Date() + " " + message.getSenderName() + " :\n" + message.getText() + "\n");
                            logger.debug("追加" + message + "至消息文本框中");
                        }
                    });
                    break;
                }
                // 接收到服务器转发的私聊信息通知
                case PRIVATE_TEXT: {
                    logger.debug("private[" + message.getSenderName() + "]" + "==> " + message.getText());
                    executor.execute(() -> {
                        if (controller != null) {
                            controller.appendText("private " + new Date() + " " + message.getSenderName() + " :\n" + message.getText() + "\n");
                            logger.debug("追加" + message + "至消息文本框中");
                        }
                    });
                    break;
                }
                // 接收到服务器拒绝登录通知
                case REFUSE_LOGIN: {
                    logger.warn(connection.getRemoteInetSocketAddress() + "已拒绝您的连接");
                    executor.execute(() -> {
                        if (controller != null) {
                            controller.appendText("[system] " + new Date() + " 已拒绝您的连接 :\n" + message.getText() + "\n");
                            logger.debug("追加" + message + "至消息文本框中");
                        }
                    });
                    break;
                }
                // 接收到用户上线通知
                case USER_ONLINE: {
                    String username = message.getText();
                    logger.debug("服务器==>" + "已上线");
                    if (users != null) {
                        users.remove(username);
                    }
                    executor.execute(() -> {
                        if (controller != null) {
                            controller.appendText("[system] " + new Date() + " 上线通知: [" + username + "]" + "\n");
                            logger.debug("追加" + message + "至消息文本框中");
                        }
                    });
                    break;
                }
                // 接收到用户下线通知
                case USER_OFFLINE: {
                    String username = message.getText();
                    logger.debug("服务器==>" + "已下线");
                    if (users != null) {
                        users.remove(username);
                    }
                    executor.execute(() -> {
                        if (controller != null) {
                            controller.appendText("[system] " + new Date() + " 下线通知: [" + username + "]" + "\n");
                            logger.debug("追加" + message + "至消息文本框中");
                        }
                    });
                    break;
                }
                case USER_LIST: {
                    logger.debug("接收到用户列表 " + message.getUserAddrMap());
                    if(users!=null) {
                        Map<String, InetSocketAddress> userMap = message.getUserAddrMap();
                        users.clear();
                        for (String name : userMap.keySet()) {
                            InetSocketAddress address = userMap.get(name);
                            String userInfo = name + "@" + address.getAddress().getHostAddress();
                            users.add(userInfo);
                        }
                        logger.info("成功更新用户列表"+users);
                    }
                    break;
                }
                default:
            }
        } catch (Exception e) {
            throw e;
        } finally {
            lock.unlock();
        }

    }

    public void close(){
        try {
            this.connection.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
