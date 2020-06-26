package org.niss.client;

import org.apache.log4j.Logger;
import org.niss.ClientMain;
import org.niss.client.listener.Listener;
import org.niss.client.listener.Listeners;
import org.niss.connect.Connection;
import org.niss.connect.TcpConnection;
import org.niss.message.Message;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.net.*;

/**
 * @author Ni187
 * <p>
 * 客户端，用于与服务器的连接，以及向服务器发送消息
 */
public class Client implements AutoCloseable {
    /**
     * 用户名
     */
    public String userName;

    /**
     * 文件传送服务端口
     */
    public int filePort;

    /**
     * TCP连接或者UDP连接，用于正常会话传输消息对象
     */
    private Connection connection;

    /**
     * TCP连接，用于登录等有状态连接的操作
     */
    private TcpConnection tcpConnection;

    private Socket fileSocket;

    Logger logger = Logger.getLogger(Client.class);

    Listeners listeners;

    /**
     * 消息处理线程池
     */
    private ExecutorService exec;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setFilePort(int filePort) {
        this.filePort = filePort;
    }

    public String getUserName() {
        return userName;
    }

    public int getFilePort() {
        return filePort;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public TcpConnection getTcpConnection() {
        return tcpConnection;
    }

    public void setTcpConnection(TcpConnection tcpConnection) {
        this.tcpConnection = tcpConnection;
    }

    public Socket getFileSocket() {
        return fileSocket;
    }

    public void setFileSocket(Socket fileSocket) {
        this.fileSocket = fileSocket;
    }

    public ExecutorService getExec() {
        return exec;
    }

    public void setExec(ExecutorService exec) {
        this.exec = exec;
    }


    public void connect() throws IOException, ClassNotFoundException {
        //若连接成功，客户端首先尝试进行登录协议, 服务端将会做出响应
        if (!this.login()) {
            throw new IOException("向服务器发送用户名失败");
        }
    }

    /**
     * 检查与服务端的连接是否已经关闭
     *
     * @return true:已断开，false:连接状态
     */
    public boolean isClosed() {
        return this.tcpConnection.getSocket().isClosed();
    }

    /**
     * 基于TCP连接，发送一个包含自己用户名，本地套接字的登录类型的Message对象
     * 从与服务器进行沟通，得到允许后可以正常使用服务端进行通信
     * 服务端会返回用户名合法性信息
     *
     * @return 是否登陆成功 true：成功，false：不成功
     * @throws IOException 协商过程出现异常
     */
    private boolean login() throws IOException, ClassNotFoundException {
        Message message = Message.newMessage(Message.MessageType.LOGIN, userName, null);
        // 消息中包含一个本地的udp套接字
        message.setSocketAddress(connection.getLocalInetSocketAddress());
        try {
            // 发送请求连接消息
            logger.debug("发送请求");
            tcpConnection.sendObject(message);
            // 接受到服务器响应消息
            Message receive = tcpConnection.receiveObject(Message.class);
            logger.debug("接收到服务器响应");
            // 如果接收到的时拒绝登录消息，return false
            if (receive.getType() == Message.MessageType.REFUSE_LOGIN) {
                logger.debug(tcpConnection.getRemoteInetSocketAddress() + ": " + receive.getText());
                return false;
            } else {
                logger.debug(receive.getSenderName() + " ==> " + receive.getText());
                return true;
            }
        } catch (IOException ioExp) {
            throw new IOException("发送连接请求==>[" + connection.getRemoteInetSocketAddress() + "]出现时异常");
        }
    }

    /**
     * 启动传入的监听器列表
     *
     * @param listener 消息监听列表
     */
    public void start(Listener listener) {
        // 消息获取，通过线程池中的ServerListener做出相应反应
        logger.debug("开始监听服务端消息");
        exec.execute(listener);
        // 创建一个新的文件接受线程
        // new ClientFileListener().start();
    }


    /**
     * 发送一个请求类型信息
     *
     * @param requests 请求
     * @throws IOException            连接异常
     * @throws ClassNotFoundException 类型转换异常
     */
    public Message requestMessage(Message requests, long wait) throws IOException, ClassNotFoundException {
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connection.sendObject(requests);
        return connection.receiveObject(Message.class);
    }

    public void requestUserList() throws IOException, ClassNotFoundException {
        Message request = Message.newUserListMessage(userName, null);
        request.setSocketAddress(connection.getLocalInetSocketAddress());
        connection.sendObject(request);
        logger.info("请求服务器的用户列表");
    }

    /**
     * 发送消息对象
     *
     * @param message 消息对象
     * @param after   延迟发送时间-ms
     */
    public void sendMessage(Message message, long after) throws IOException {
        // 延时发送
        try {
            Thread.sleep(after);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connection.sendObject(message);
        logger.debug(message + " ==> " + connection.getRemoteInetSocketAddress());
    }

    public void sendFile(String targetIP, String filePath) {
        Runnable sendFiletask = () -> {
            try {
                //获取文件名
                String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
                if (fileName == null || "filename".equals(fileName)) {
                    throw new Exception();
                }

                //请求与客户端的连接
                try {
                    fileSocket = new Socket(targetIP, this.filePort);
                } catch (Exception e) {
                    ClientMain.clientController.receiveMsgBox.appendText("连接 " + targetIP + " 失败\n\n");
                    return;
                }

                //发送文件名
                DataOutputStream namesend = new DataOutputStream(fileSocket.getOutputStream());
                namesend.writeUTF(fileName);
                namesend.flush();
                Thread.sleep(100);

                //创建文件输入流
                BufferedInputStream fin = new BufferedInputStream(new FileInputStream(filePath));
                //创建输出给客户端的文件输出流
                BufferedOutputStream fout = new BufferedOutputStream(fileSocket.getOutputStream());

                ClientMain.clientController.receiveMsgBox.appendText("发送 " + fileName + " 给 " + targetIP + "\n\n");
                byte[] bytes = new byte[4096];
                int len;
                while ((len = fin.read(bytes)) != -1) {
                    //每读取len个字节输出给socket
                    fout.write(bytes, 0, len);
                    fout.flush();
                }
                ClientMain.clientController.receiveMsgBox.appendText("发送 " + filePath + " 成功\n\n");
                fileSocket.close();
            } catch (Exception ignored) {
                ClientMain.clientController.receiveMsgBox.appendText("发送 " + filePath + " 失败\n\n");
            }
        };

        Thread fileSendThread = new Thread(sendFiletask);
        fileSendThread.start();
    }


    /**
     * 关闭与服务器的TCP与UDP连接
     */
    @Override
    public void close() {
        try {
            connection.close();
            tcpConnection.close();
        } catch (Exception e) {
            logger.info("客户端连接关闭失败", e);
        }
    }

}
