package com.company;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Created by IEEE on 5/9/2015.
 */
public class ViewController {
    private VBox leftBox;
    private VBox rightBox;
    private TextField speed;
    private TextField radius;
    private Button stop;
    private Button send;

    private ImageView faceView;
    private Image faceImage;

    public HBox root;

    private final KobukiController k;

    public ViewController() {
        setupLayout();

        root = new HBox();
        root.getChildren().addAll(leftBox, rightBox);

        root.setPrefWidth(600);
        root.setPrefHeight(400);

        // Setup robot
        k = new KobukiController("COM27");
    }

    private void setupLayout() {
        leftBox = new VBox();
        rightBox = new VBox();

        // Photo viewer and label
        VBox faceBox = new VBox();

        Label faceLabel = new Label("Kobuki Status");

        faceView = new ImageView();
        faceImage = new Image("path");
        faceView.setImage(faceImage);

        // Speed inputs
        HBox speedBox = new HBox();
        speedBox.setPadding(new Insets(10));

        Label speedLabel = new Label("Speed");

        speed = new TextField();
        speed.setText("0");
//        speed.setPadding(new Insets(5));

        speedBox.getChildren().addAll(speedLabel, speed);

        // Radius inputs
        HBox radiusBox = new HBox();
        radiusBox.setPadding(new Insets(10));

        Label radiusLabel = new Label("Radius");

        radius = new TextField();
        radius.setText("0");

        radiusBox.getChildren().addAll(radiusLabel, radius);

        HBox buttonBox = new HBox();

        stop = new Button();
        stop.setText("Stop");
        stop.setOnMouseClicked((event) -> k.stop());

        send = new Button();
        send.setText("Send");
        send.setOnMouseClicked((event) -> {
            short s, r;

            try {
                s = Short.parseShort(speed.getText());
                r = Short.parseShort(radius.getText());
            } catch (Exception e) {
                s = 0;
                r = 0;
            }

            k.move(s, r);
        });

        buttonBox.getChildren().addAll(send, stop);

        leftBox.getChildren().addAll(speedBox, radiusBox, buttonBox);
    }
}
