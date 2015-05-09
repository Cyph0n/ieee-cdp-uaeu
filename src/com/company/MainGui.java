package com.company;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import edu.ycp.robotics.KobukiRobot;

import java.util.concurrent.*;

public class MainGui extends Application {
    private Stage primaryStage;

    public void start(Stage s) {
        primaryStage = s;

        primaryStage.setTitle("Kobuki Control Panel");
        primaryStage.setResizable(false);

        ViewController c = new ViewController();

        Scene scene = new Scene(c.root);

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest((event) -> System.exit(0));

        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
