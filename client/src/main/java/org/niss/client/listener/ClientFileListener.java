package org.niss.client.listener;

import org.niss.ClientMain;

import java.net.*;
import java.io.*;

public class ClientFileListener implements Runnable{
    private ServerSocket receiveSocket;
    public ClientFileListener(){
        try{
            receiveSocket = new ServerSocket(55555);
        }catch (Exception e){
            receiveSocket = null;
        }
    }

    @Override
    public void run() {
        try {
            if(receiveSocket == null){
                System.out.println("无接受socket");
                return ;
            }
            while (true) {
                System.out.println("waiting a new file...");
                Socket fileSocket = receiveSocket.accept();
                try {
                    //接受其他客户端的连接
                    //获取发送者的ip
                    InetAddress ip = fileSocket.getInetAddress();

                    //获取包含文件名的输入流
                    DataInputStream nameStream = new DataInputStream(fileSocket.getInputStream());

                    //获取文件名
                    String fileName = nameStream.readUTF();
                    ClientMain.clientController.receiveMsgBox.appendText(ip.getHostAddress() + " 向你发送文件：" + fileName+"\n\n");


                    File file = new File(fileName);

                    //创建文件输出流
                    BufferedOutputStream bfout = new BufferedOutputStream(new FileOutputStream(file));

                    //获取文件输入流
                    BufferedInputStream bfin = new BufferedInputStream(fileSocket.getInputStream());
                    byte[] bytes = new byte[4096];
                    int len;
                    while ((len = bfin.read(bytes)) != -1) {
                        bfout.write(bytes, 0, len);
                        bfout.flush();
                    }
                    ClientMain.clientController.receiveMsgBox.appendText(ip.getHostAddress() + " ：" + fileName + " 已接受\n\n");
                } catch (Exception e) {
                    ClientMain.clientController.receiveMsgBox.appendText("接受失败\n\n");
                }finally {
                    fileSocket.close();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("创建文件接受进程失败");
        }
    }


}
