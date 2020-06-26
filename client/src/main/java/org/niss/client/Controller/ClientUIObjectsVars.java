package org.niss.client.Controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/*
*  ControllerObjectVars类
*  包含窗口的各个变量
*/
class ClientUIObjectsVars {

    @FXML//用户名文本框
    public TextField userName;

    @FXML//目标用户文本框
    public TextField targetName;

    @FXML//文件接受目标文本框
    public TextField sendFileTargetName;

    @FXML//接受消息区域
    public TextArea receiveMsgBox;

    @FXML//消息区域
    public TextArea msgBox;

    @FXML//文件路径填写区域
    public TextField fileBox;

    @FXML//连接按钮
    public Button connectServerBtn;

    @FXML//发送消息按钮
    public Button sendMsgBtn;

    @FXML//发送文件按钮
    public Button sendFileBtn;

    @FXML//清除按钮
    public Button clearMsgBtn;

    @FXML
    public Button refreshUserListBtn;

    @FXML//状态标签
    public Label statusLabel;

    @FXML//聊天状态标签
    public Label chatStatusLabel;

    @FXML
    protected ListView<String> userList;
}
