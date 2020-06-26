package org.niss.connect;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Ni187
 */
public interface Connection extends Closeable,AutoCloseable  {

    /**
     * 从对方接收到一个对象
     * @return 对方发送的对象
     * @throws IOException 连接或接受异常
     * @throws ClassNotFoundException 类型转换异常
     */
    Object receiveObject() throws IOException,ClassNotFoundException;

    /**
     * 从对方接收到一个对象,并转化为预期类型
     * @param tClass 预期类型
     * @param <T> 预期类型
     * @return 对方发送的对象
     * @throws IOException 连接或接受异常
     * @throws ClassNotFoundException 类型转换异常
     */
    <T> T receiveObject(Class<T> tClass) throws IOException,ClassNotFoundException;

    /**
     * 发送一个对象至连接方
     * @param obj 待发送对象
     * @throws IOException 连接或发送异常
     */
    void sendObject(Object obj) throws IOException;


    /**
     * 返回连接的SocketAddress
     * @return socketAddress
     */
    InetSocketAddress getRemoteInetSocketAddress();

    /**
     * 获取本地绑定的SocketAddress
     * @return 网络连接本地绑定的SocketAddress
     */
    InetSocketAddress getLocalInetSocketAddress();

}
