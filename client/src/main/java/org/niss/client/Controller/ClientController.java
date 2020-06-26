package org.niss.client.Controller;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.apache.log4j.Logger;
import org.niss.ClientMain;
import org.niss.client.Client;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.niss.client.listener.Listener;
import org.niss.client.listener.Listeners;
import org.niss.client.listener.ServerListener;
import org.niss.message.Message;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;


/**
 * @author Ni187
 *
 * Controller类
 * 用于窗口变量接受响应事件，并调用Client类中方法，完成其功能实现，显示在客户端
 */

public class ClientController extends ClientUIObjectsVars implements Initializable {

    ApplicationContext appContext = new ClassPathXmlApplicationContext("application-config.xml");

    private Client client;
    private Listener listener;
    private boolean connectionStatus = false;
    private Thread backgroundThread;
    private ObservableList<String> userInfoList;
    private List<String> userStoreList;

    Logger logger = Logger.getLogger(ClientController.class);

    /**
     * 初始化设置GUI组件属性
     *
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //接收消息框不可改
        this.receiveMsgBox.setEditable(false);
        this.receiveMsgBox.setWrapText(true);
        this.msgBox.setWrapText(true);
        userStoreList = new ArrayList<>();
        this.userInfoList = new ObservableListWrapper<>(userStoreList);
        this.userList.setItems(userInfoList);
        this.chatStatusLabel.setText("please connect to start chatting.");
    }

    public void setClient(Client client) {
        this.client = client;
    }


    /**
     * 点击connect连接服务器
     * @param event connect点击事件
     */
    @FXML
    private void connectServerBtnAction(ActionEvent event) {
        //如果没有连接则尝试连接Server
        if (!this.connectionStatus) {
            // 如果
            if (client != null) {
                client.close();
            }
            try {
                //创建一个Runnable
                Runnable task = () -> {
                    // 获取线程对象
                    this.client = appContext.getBean("client", Client.class);
                    this.listener = appContext.getBean("listener", Listener.class);
                    this.client.setUserName(userName.getText());
                    logger.info(client.getConnection());
                    logger.info(this.client.userName + "==start=>" + client.getConnection().getRemoteInetSocketAddress());
                    try {
                        Platform.runLater(() -> ClientMain.clientController.statusLabel.setText("Connecting"));
                        this.client.connect();
                        logger.info("connected");
                        listener.setTextArea(receiveMsgBox);
                        listener.setName(client.getUserName());
                        listener.setUserList(this.userInfoList);
                        this.client.start(listener);
                        //连接后设置连接状态为true
                        Platform.runLater(() -> {
                            ClientMain.clientController.chatStatusLabel.setText("Server connected");
                            ClientMain.clientController.statusLabel.setText("Connected");
                            ClientMain.clientController.connectServerBtn.setText("Disconnect");
                        });
                    } catch (Exception e) {
                        this.userName.setText(null);
                        //否则，UI对应改变显示
                        Platform.runLater(() -> {
                            ClientMain.clientController.statusLabel.setText("Connect");
                            ClientMain.clientController.chatStatusLabel.setText("Can not connect to the server");
                        });
                    }
                };
                //在后台运行一个线程任务
                this.backgroundThread = new Thread(task);
                //开始线程任务
                this.backgroundThread.start();
                //线程暂停5ms
                Thread.sleep(500);

            } catch (Exception exp) {
                logger.warn("连接服务器时发生异常", exp);
                this.statusLabel.setText("Connect Error!");
            }
            //连接后设置连接状态为true
            this.connectionStatus = true;
        } else {
            client.close();
            try {
                this.backgroundThread.interrupt();
                Thread.sleep(500);
            } catch (Exception ignored) {
            }
            this.connectionStatus = false;
            this.statusLabel.setText("Server status err");
            this.connectServerBtn.setText("Connect");
            this.chatStatusLabel.setText("please connect to chat.");
        }
    }

    /**
     * 发送消息事件
     */
    @FXML
    private void sendMsgBtnAction(ActionEvent event) {
        //如果处于连接状态则发送消息，否则在聊天状态标签显示please connect first
        if (this.connectionStatus) {
            // 获取文本框中的消息
            String msg = msgBox.getText().trim();
            if("".equals(msg)){
                return ;
            }
            try {
                if (targetName.getText()!=null) {
                    Message message = Message.newMessage(Message.MessageType.PUBLIC_TEXT, client.getUserName(), msg);
                    // 设置本地的UDP套接字
                    message.setSocketAddress(client.getConnection().getLocalInetSocketAddress());
                    this.client.sendMessage(message, 10);
                    Platform.runLater(() -> ClientMain.clientController.msgBox.setText(null));
                    Platform.runLater(() -> ClientMain.clientController.chatStatusLabel.setText("Message sent"));
                    ClientMain.clientController.receiveMsgBox.appendText("\n[public]" + " You ==>\n" + msg + "\n");
                } else {
                    Collection<String> usernames = getTargetUserNames();
                    Message message = Message.newMessage(Message.MessageType.PRIVATE_TEXT, client.getUserName(), msg, usernames);
                    message.setUsernames(usernames);
                    this.client.sendMessage(message, 10);
                    ClientMain.clientController.receiveMsgBox.appendText("\n[private]" + " You ==>\n" + msg + "\n");
                }
            } catch (IOException e) {
                logger.info(e);
            }
            this.msgBox.setText(null);
        } else {
            this.chatStatusLabel.setText("Please connect first!");
        }
        //暂停100ms
        try {
            Thread.sleep(100);
        } catch (Exception ignored) {
        }
        //销毁事件
        event.consume();
    }

    /**
     * 获取目标用户名
     *
     * @return 目标用户名列表
     */
    public Collection<String> getTargetUserNames() {
        String usernamesStr = targetName.getText();
        return new ArrayList<>(Arrays.asList(usernamesStr.split("\\|")));
    }

    @FXML
    private void refreshUserList(ActionEvent event){
        try {
            client.requestUserList();
        }catch (IOException|ClassNotFoundException e){
            logger.info("用户列表请求失败",e);
        }
    }

    /**
     * 发送文件
     */
    @FXML
    private void sendFileBtnAction(ActionEvent event) {
        //如果处于连接状态则发送消息，否则在聊天状态标签显示please connect first
        if (this.connectionStatus) {
            this.client.sendFile(this.sendFileTargetName.getText(), this.fileBox.getText().trim());
        } else {
            this.chatStatusLabel.setText("Please connect first!");
        }
        // 暂停1ms
        try {
            Thread.sleep(100);
        } catch (Exception ignored) {
        }
        //销毁事件
        event.consume();
    }

    /**
     * 清除按钮上的事件：清除目标name、发送、接受消息区域的内容
     */
    @FXML
    private void clearMsgBtnAction(ActionEvent event) {
        this.targetName.setText(null);
        this.receiveMsgBox.setText(null);
        this.fileBox.setText(null);
        this.sendFileTargetName.setText(null);
        this.msgBox.setText(null);
    }

}

