package com.company;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainGui extends Application {
    private Stage primaryStage;

    public void start(Stage s) {
        primaryStage = s;

        primaryStage.setTitle("Kobuki Control Panel");
        primaryStage.setResizable(false);

        ViewController c = new ViewController();

        Scene scene = new Scene(c.root);

        primaryStage.setScene(scene);
//        primaryStage.setWidth(800);
//        primaryStage.setHeight(600);
        primaryStage.setOnCloseRequest((event) -> System.exit(0));

        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
