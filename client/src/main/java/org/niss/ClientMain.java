package org.niss;

import org.niss.client.Controller.ClientController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

    public static ClientController clientController;

    //Client程序入口
    public static void main(String[] args) {
        launch(args);
    }

    //加载FXML文件创建UI
    @Override
    public void start(Stage primaryStage) throws Exception{
        //加载FXML文件，并绑定控制器
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/UI.fxml"));
        Parent root = fxmlLoader.load();
        clientController = fxmlLoader.getController();
        //设置标题
        primaryStage.setTitle("Java Chat Application (Client)");
        //创建所加载的FXML的场景
        Scene scene = new Scene(root);
//
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
//        //scene.getStylesheets().clear();
//        //设置场景到舞台
        primaryStage.setScene(scene);
        //设置舞台尺寸不可改变
        primaryStage.setResizable(false);
        //设置关闭请求
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        //设置是否隐式关闭
        Platform.setImplicitExit(true);
        //展现舞台
        primaryStage.show();
    }
}
