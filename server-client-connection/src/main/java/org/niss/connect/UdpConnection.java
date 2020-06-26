package org.niss.connect;

import org.niss.message.Message;

import java.io.*;
import java.net.*;

/**
 * @author ni187
 * <p>
 * 用于UDP连接，接受与发送对象
 */
public class UdpConnection implements Connection {

    private DatagramSocket datagramSocket;

    private InetSocketAddress remoteAddress;

    private int localPort;

    private int receiveBufferSize;

    /**
     * 缓冲区大小
     */
    private static final int RECEIVE_BUFFER_SIZE = 1 << 16;

    public UdpConnection(int localPort, String host, int serverPort, int receiveBufferSize) throws IOException {
        this.datagramSocket = new DatagramSocket(localPort);
        this.remoteAddress = new InetSocketAddress(host, serverPort);
        this.receiveBufferSize = receiveBufferSize;
        this.localPort = localPort;
    }

    public UdpConnection(int localPort, String host, int serverPort) throws IOException {
        this(localPort, host, serverPort, RECEIVE_BUFFER_SIZE);
    }

    public UdpConnection(int localPort, int receiveBufferSize) throws IOException {
        this.datagramSocket = new DatagramSocket(localPort);
        this.localPort = localPort;
        this.receiveBufferSize = receiveBufferSize;
    }

    public UdpConnection(InetSocketAddress remoteAddress) throws IOException {
        this.datagramSocket = new DatagramSocket(remoteAddress);
        this.remoteAddress = remoteAddress;
    }

    /**
     * 用于接受对象，如果想要发送对象，需要在发送时指定对方接受地址与端口
     *
     * @param port 本地udp占用端口
     * @throws IOException 占用端口失败
     */
    public UdpConnection(int port) throws IOException {
        this.datagramSocket = new DatagramSocket(port);
    }

    public DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    public void setDatagramSocket(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    @Override
    public InetSocketAddress getRemoteInetSocketAddress() {
        return remoteAddress;
    }

    @Override
    public InetSocketAddress getLocalInetSocketAddress() {
        return new InetSocketAddress(datagramSocket.getLocalAddress(), localPort);
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void sendObject(Object dto) throws IOException {
        sendObject(dto, remoteAddress);
    }

    /**
     * 发送对象至对方
     *
     * @param dto           数据传输对象
     * @param remoteAddress 远程udp地址
     * @throws IOException 数据传送异常
     */
    public void sendObject(Object dto, InetSocketAddress remoteAddress) throws IOException {
        //准备数据，将数据转成字节数组
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
        oos.writeObject(dto);
        oos.flush();
        byte[] dataToSend = bos.toByteArray();
        //封装包裹
        System.out.println(remoteAddress);
        DatagramPacket packet = new DatagramPacket(dataToSend, 0, dataToSend.length, remoteAddress);
        //发送包裹
        datagramSocket.send(packet);
    }

    /**
     * 接受一个对象，并转换为预期的类型
     *
     * @param tClass 预期类型
     * @param <T>    类型
     * @return 接收到的并转化的对象
     * @throws IOException            数据接受异常
     * @throws ClassNotFoundException 类型转换错误
     */
    @Override
    public <T> T receiveObject(Class<T> tClass) throws IOException, ClassNotFoundException {
        Object obj = receiveObject();
        return (T) obj;
    }

    /**
     * 从UDP传输中接受一个对象
     *
     * @return 接收到的并转化的对象
     * @throws IOException 数据接受异常
     */
    @Override
    public Object receiveObject() throws IOException, ClassNotFoundException {
        //封装包裹
        byte[] data = new byte[receiveBufferSize];
        DatagramPacket packet = new DatagramPacket(data, 0, data.length);
        //接受包裹
        datagramSocket.receive(packet);
        //分析数据
        byte[] receiveData = packet.getData();
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(receiveData)));
        return ois.readObject();
    }

    /**
     * 通过UDP从远方接受一个Message对象
     *
     * @return Message对象
     * @throws IOException            数据传输异常
     * @throws ClassNotFoundException 接受到的不是Message对象
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        byte[] data = new byte[receiveBufferSize];
        DatagramPacket packet = new DatagramPacket(data, 0, data.length);
        datagramSocket.receive(packet);
        byte[] receiveData = packet.getData();
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(receiveData)));
        Object receive = ois.readObject();

        if (receive instanceof Message) {
            Message message = (Message) receive;
            // 将远程发送方IP与端口的注入message对象中
            message.setSocketAddress(new InetSocketAddress(packet.getAddress(), packet.getPort()));
            return message;
        } else {
            throw new ClassCastException("接收到的类型并非是Message");
        }
    }

    @Override
    public String toString() {
        return "UdpConnection{" +
                "datagramSocket=" + datagramSocket +
                ", remoteAddress=" + remoteAddress +
                ", receiveBufferSize=" + receiveBufferSize +
                '}';
    }

    /**
     * 关闭UDP接受功能
     */
    @Override
    public void close() {
        this.datagramSocket.close();
    }

}
