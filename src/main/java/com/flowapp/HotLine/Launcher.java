package com.flowapp.HotLine;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Launcher extends Application {

    public static void main(String[] args) {
        HotLine.hotLine();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Button btn = new Button();
        btn.setText("Hello World!");
        btn.setOnAction( e -> {
            System.out.println("Hello World");
        });
        StackPane group = new StackPane();
        group.getChildren().add(btn);
        Scene scene = new Scene(group, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
