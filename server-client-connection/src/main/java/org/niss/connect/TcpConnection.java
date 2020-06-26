package org.niss.connect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author Ni187
 *
 * 客户端会话连接
 * 保存输入流输出流
 */
public class TcpConnection implements Connection{
    private Socket socket;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public TcpConnection(String host,int port)throws IOException{
        this(new Socket(host,port));
    }

    public TcpConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public TcpConnection(Socket socket, ObjectOutputStream out, ObjectInputStream in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public void setOut(ObjectOutputStream out) {
        this.out = out;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public void setIn(ObjectInputStream in) {
        this.in = in;
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        socket.bind(bindpoint);
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public int getPort() {
        return socket.getPort();
    }

    public void shutdownInput() throws IOException {
        socket.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }

    @Override
    public InetSocketAddress getRemoteInetSocketAddress(){
        return new InetSocketAddress(socket.getInetAddress(),socket.getPort());
    }

    @Override
    public InetSocketAddress getLocalInetSocketAddress(){
        return new InetSocketAddress(socket.getLocalAddress(),socket.getLocalPort());
    }

    @Override
    public void sendObject(Object obj) throws IOException{
        this.out.writeObject(obj);
        this.out.flush();
    }

    public void sendObjectNotInstant(Object obj) throws IOException{
        this.out.writeObject(obj);
    }

    @Override
    public Object receiveObject() throws IOException,ClassNotFoundException{
        return in.readObject();
    }

    @Override
    public<T> T receiveObject(Class<T> tClass ) throws IOException ,ClassNotFoundException{
        Object obj = in.readObject();
        return (T)obj;
    }

    public void flush() throws IOException{
        this.out.flush();
    }

    @Override
    public void close() throws IOException{
        out.close();
        in.close();
        socket.close();
    }
}
