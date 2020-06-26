package org.niss.message;

import java.io.PrintStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ni187
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 18724787072L;

    public enum MessageType implements Serializable {
        /**
         * 私聊信息，文本信息
         */
        PRIVATE_TEXT,

        /**
         * 公聊信息，文本信息
         */
        PUBLIC_TEXT,

        /**
         * 请求连接
         */
        LOGIN,

        /**
         * 拒绝连接
         */
        REFUSE_LOGIN,

        /**
         * 文件
         */
        FILE,

        /**
         * 用户上线
         */
        USER_ONLINE,

        /**
         * 用户下线
         */
        USER_OFFLINE,

        /**
         * 要求用户列表
         */
        USER_LIST,
    }

    /**
     * 发送者用户名
     */
    private final String senderName;

    private InetSocketAddress socketAddress;

    /**
     * 消息类型
     */
    private final MessageType type;

    private Date date;

    /**
     * 文本信息
     */
    private final String text;

    /**
     * 用户名
     */
    private Collection<String> usernames;

    /**
     * 用户列表以及对应的TCP套接字
     */
    private ConcurrentHashMap<String,InetSocketAddress> userAddrMap;


    public ConcurrentHashMap<String, InetSocketAddress> getUserAddrMap() {
        return userAddrMap;
    }

    /**
     * 构造基本的信息对象，包含类型，发送方名字，文本信息，构造的时间
     * @param type 类型
     * @param senderName 发送方
     * @param text 发送文本
     */
    private Message(MessageType type, String senderName, String text){
        this.text = text;
        this.senderName = senderName;
        this.type = type;
        date = new Date();
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public void setSocketAddress(InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MessageType getType() {
        return type;
    }

    public Date getDate(){
        return date;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

    public Collection<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(Collection<String> usernames){
        this.usernames = usernames;
    }

    public void setUserAddrMap(ConcurrentHashMap<String, InetSocketAddress> userAddrMap) {
        this.userAddrMap = userAddrMap;
    }



    public static Message newMessage(MessageType type, String senderName,String text){
        return new Message(type,senderName,text);
    }

    public static Message newMessage(MessageType type, String senderName, String text, Collection<String> usernames){
        Message message = null;
        switch (type){
            //TODO 文件之后考虑
            case FILE:break;
            case PUBLIC_TEXT:
            case USER_ONLINE:
            case USER_OFFLINE:
            case LOGIN:
            case REFUSE_LOGIN:
                message = new Message(type,senderName,text);
                return message;
            case PRIVATE_TEXT:
                message = new Message(type,senderName,text);
                message.setUsernames(usernames);
                return message;
            default:
        }
        return null;
    }



    /**
     * 构造一个公开文本消息
     * @param senderName 发送者用户名
     * @param text 消息文本
     * @return 公开消息
     */
    public static Message newPublicMessage(String senderName,String text){
        return new Message(MessageType.PUBLIC_TEXT,senderName,text);
    }

    /**
     * 构造一个包含用户列表的消息
     * @param senderName 发送者用户名
     * @param userAddrMap 用户列表：用户名==>用户本地套接字
     * @return 包含用户列表的消息
     */
    public static Message newUserListMessage(String senderName,ConcurrentHashMap<String,InetSocketAddress> userAddrMap) {
        Message message = new Message(MessageType.USER_LIST,senderName,null);
        message.setUserAddrMap(userAddrMap);
        return message;
    }

    /**
     * 构造私有消息，包含发送者用户名，消息文本，接受者用户名
     * @param senderName 发送者用户名
     * @param text 文本消息
     * @param usernames 接收者们的用户名
     * @return 私有消息
     */
    public static Message newPrivateMessage(String senderName,String text,Collection<String> usernames){
        Message message = new Message(MessageType.PRIVATE_TEXT,senderName,text);
        message.setUsernames(usernames);
        return message;
    }


    @Override
    public String toString() {
        return "Message{" +
                "senderName='" + senderName + '\'' +
                ", socketAddress=" + socketAddress +
                ", type=" + type +
                ", text='" + text + '\'' +
                ", usernames=" + usernames +
                ", userAddrMap=" + userAddrMap +
                '}';
    }
}
