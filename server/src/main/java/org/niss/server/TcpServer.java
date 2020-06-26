package org.niss.server;

import org.apache.log4j.Logger;
import org.niss.connect.Connection;
import org.niss.connect.UdpConnection;
import org.niss.message.Message;
import org.niss.connect.TcpConnection;
import org.niss.server.listener.TcpClientListener;
import org.niss.server.listener.UdpClientListener;

import java.net.*;
import java.io.*;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Ni187
 */
public class TcpServer implements AutoCloseable {

    private ServerSocket serverSocket;

    private UdpConnection udpConnection;

    /**
     * 用户会话管理线程池
     */
    private ExecutorService exec;

    /**
     * 保存用户名至会话连接的键值对
     */
    private ConcurrentHashMap<String,TcpConnection> tcpSessionConnectionMap;

    /**
     * 保存用户名至消息连接的键值对
     */
    private ConcurrentHashMap<String,InetSocketAddress> messageAddressMap;


    /**
     * 服务器已关闭标识
     */
    private boolean closed = false;

    /**
     * 日志记录
     */
    Logger logger = Logger.getLogger(TcpServer.class);

    public TcpServer() {
    }

    public void setMessageConnectionMap(ConcurrentHashMap<String, InetSocketAddress> messageAddressMap) {
        this.messageAddressMap = messageAddressMap;
    }

    public InetSocketAddress getClientInetSocketAddr(String username) {
        return messageAddressMap.get(username);
    }

    public ConcurrentHashMap<String, InetSocketAddress> getUsernameInetAddrMap() {
        return this.messageAddressMap;
    }

    /**
     * 通过用户名获取用户的TCP连接套接字
     * @param username 用户名
     * @return 用户名对应的套接字
     */
    public InetSocketAddress getSessionInetSocketAddr(String username){
        return tcpSessionConnectionMap.get(username).getRemoteInetSocketAddress();
    }

    public Collection<InetSocketAddress> getAllClientInetSocketAddr(){
        return messageAddressMap.values();
    }

    public void setUdpConnection(UdpConnection udpConnection) {
        this.udpConnection = udpConnection;
    }

    public TcpConnection getTcpConnectionByName(String name) {
        return tcpSessionConnectionMap.get(name);
    }

    public Collection<TcpConnection> getTcpConnections(){
        return this.tcpSessionConnectionMap.values();
    }

    public Set<String> getUsers(){
        return this.tcpSessionConnectionMap.keySet();
    }

    /**
     * 移除有关传入用户名的连接
     * @param username 用户名
     */
    public void removeClientConnection(String username){
        tcpSessionConnectionMap.remove(username);
        messageAddressMap.remove(username);
    }
    public UdpConnection getUdpConnection(){
        return this.udpConnection;
    }

    public void setTcpSessionConnectionMap(ConcurrentHashMap<String, TcpConnection> tcpSessionConnectionMap) {
        this.tcpSessionConnectionMap = tcpSessionConnectionMap;
    }

    public void setExec(ExecutorService exec) {
        this.exec = exec;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public ExecutorService getExec() {
        return exec;
    }

    public ConcurrentHashMap<String, TcpConnection> getTcpSessionConnectionMap() {
        return tcpSessionConnectionMap;
    }

    public ConcurrentHashMap<String, InetSocketAddress> getMessageAddressMap() {
        return messageAddressMap;
    }

    public void start(){

        // 开启一个udp监听服务，监听客户端发送的udp数据包
        if(udpConnection!=null){
            logger.info("启动UDP消息监听进程:"+udpConnection);
            exec.execute(new UdpClientListener(udpConnection,this));
        }

        // 循环接受客户端的TCP连接，并针对每一个TCP连接进行处理
        while (!closed) {
            try {
                //通过循环不断接受新的连接，储存在线程池中
                Socket socket = serverSocket.accept();
                InetAddress address = socket.getInetAddress();
                exec.execute(()-> {
                    logger.debug("["+socket.getInetAddress() + "] 尝试连接到服务器");
                    // 获取客户端name
                    String userName;
                    TcpConnection tcpSessionConnection;
                    try {
                        tcpSessionConnection = new TcpConnection(socket);
                        // 尝试获取客户端的连接
                        userName = this.requireUserName(tcpSessionConnection);
                        if(userName==null){
                            logger.warn("尝试从@["+address+"] 获取用户名失败");
                            return ;
                        }
                        logger.debug(userName + "@[" + address + "] 已连接");
                        //向用户集合中添加name:TcpConnection键值对
                        tcpSessionConnectionMap.put(userName,tcpSessionConnection);
                    } catch (Exception e) {
                        logger.debug(address + "连接异常，已中断连接",e);
                        return ;
                        //若用户名设置异常则中断连接
                    }

                    // 将用户上线的消息通知到所有的客户端
                    notifyUserStatus(userName,true);
                    /*
                     * 启动一个线程，由线程来处理客户端的请求，这样可以再次监听
                     * 下一个客户端的连接
                     */
                    // 通过线程池来分配线程
                    exec.execute(new TcpClientListener(tcpSessionConnection, userName, this));
                });
            } catch (Exception e) {
                logger.debug("等待客户端连接时发生异常，准备等待下一次连接",e);
            }
        }
    }

    /**
     * 从新的socket连接中获得客户端用户名，并作简单处理
     * 若用户名不合法，则通知用户重新设置用户名，并抛出异常
     */
    public String requireUserName(TcpConnection connection){
        try{
            Message message = connection.receiveObject(Message.class);
            String name = message.getSenderName();

            //若客户端用户名异常或重名或包含非法字符
            if (checkUserName(name)) {
                //向客户端发送成功通知
                logger.debug(name+" 校验成功");
                connection.sendObject(Message.newMessage(Message.MessageType.PUBLIC_TEXT, "Server System", "[系统通知]登陆成功，欢迎加入聊天室"));
                // 将用户名与用户的UDP套接字添加到map中
                InetSocketAddress inetSocketAddress = message.getSocketAddress();
                if("0.0.0.0".equals(inetSocketAddress.getAddress().getHostName())){
                    inetSocketAddress = new InetSocketAddress("127.0.0.1",inetSocketAddress.getPort());
                }
                logger.info("添加 username="+name+", inetAddr="+inetSocketAddress);
                messageAddressMap.put(name, inetSocketAddress);
                return name;
            } else {
                //向客户端发送失败通知
                logger.debug(name+" 校验失败");
                connection.sendObject(Message.newMessage(Message.MessageType.REFUSE_LOGIN, "Server System", "[系统通知]昵称设置失败，请重新设置"));
                return null;
            }
        }catch (ClassNotFoundException|IOException e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    /**
     * 检测用户名是否包含特殊字符
     *
     * @param name username
     * @return 暂定为true
     */
    private boolean checkUserName(String name) {
        // TODO 用户连接协商处理
        return true;
        // return !"".equals(name) && !userMap.containsKey(name) && !name.contains(":") && !name.contains("&") && !name.contains("|") && !name.contains("\\");
    }

    /**
     * 将用户上下线消息发送到所有链接到服务器的客户端
     */
    public void notifyUserStatus(String username,boolean online) {
        String status = online? " 已上线":" 已下线";
        logger.debug("开始通知所有人 "+username + status);
        //通过循环遍历HashMap来获取User.socket,从而发送信息
        for (Connection connection : tcpSessionConnectionMap.values()) {
            try {
                Message message = Message.newMessage(
                        online? Message.MessageType.USER_ONLINE : Message.MessageType.USER_OFFLINE
                        , "system",username);
                connection.sendObject(message);
                logger.debug("发送用户"+ username + (online?"上线":"下线")+"通知==>"+connection.getRemoteInetSocketAddress());
            } catch (Exception e) {
                logger.debug("推送用户登陆状态时发生异常: ",e);
            }
        }
    }

    /**
     * 关闭连接
     * 关闭serverSocket
     */
    @Override
    public void close() {
        try {
            // 标识服务socket已关闭，禁止其他客户端连接
            closed = true;
            // 关闭每一个开启的socket
            for (Connection connection : tcpSessionConnectionMap.values()) {
                connection.close();
            }
            if (serverSocket!=null && serverSocket.isBound()) {
                serverSocket.close();
                logger.debug("关闭服务器");
            }
            udpConnection.close();
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

}
