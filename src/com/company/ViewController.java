package com.company;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ViewController {
    private VBox leftBox;
    private VBox rightBox;
    private TextField speed;
    private TextField radius;
    private TextField distance;
    private Button stop;
    private Button send;
    private ComboBox<String> menu;

    private Button taskOne;
    private Button taskTwo;

    private Button stopCamera;
    private Button startCamera;

    private ImageView faceView;
    private ImageView cameraView;

    public TextField min;
    public TextField max;

    public HBox root;

    private final KobukiController k;

    private final KobukiCamera cam;

    public ViewController() {
        root = new HBox();
        root.setPrefWidth(800);
        root.setPrefHeight(600);

        setupLayout();

        root.getChildren().addAll(leftBox, rightBox);

        // Setup robot controller
        k = new KobukiController("COM3", faceView, cameraView);

        cam = k.getCamera();
        cam.min = min;
        cam.max = max;
    }

    private void setupLayout() {
        // Setup menu
        ObservableList<String> items = FXCollections.observableArrayList();
        items.addAll("Move", "Rotate", "Move for Distance");

        menu = new ComboBox<>();
        menu.setItems(items);

        leftBox = new VBox();
        leftBox.setPrefWidth(root.getPrefWidth() * (2.0 / 5));

        rightBox = new VBox();
        rightBox.setPrefWidth(root.getPrefWidth() * (3.0/5));

        // Photo viewer and label
        VBox faceBox = new VBox();
        Label faceLabel = new Label("Kobuki Status");
        faceView = new ImageView();

        faceBox.getChildren().addAll(faceLabel, faceView);

        // Camera viewer
        cameraView = new ImageView();
        cameraView.setFitWidth(leftBox.getPrefWidth());
        cameraView.setPreserveRatio(true);

        // Speed inputs
        HBox speedBox = new HBox();
        speedBox.setPadding(new Insets(10));

        Label speedLabel = new Label("Speed");

        speed = new TextField();
        speed.setText("0");

        speedBox.getChildren().addAll(speedLabel, speed);

        // Radius inputs
        HBox radiusBox = new HBox();
        radiusBox.setPadding(new Insets(10));

        Label radiusLabel = new Label("Radius");

        radius = new TextField();
        radius.setText("0");

        radiusBox.getChildren().addAll(radiusLabel, radius);

        // Distance inputs
        HBox distBox = new HBox();
        distBox.setPadding(new Insets(10));

        Label distLabel = new Label("Distance");

        distance = new TextField();
        distance.setText("0");

        distBox.getChildren().addAll(distLabel, distance);

        // Control buttons
        HBox buttonBox = new HBox();

        stop = new Button();
        stop.setText("Stop");
        stop.setOnMouseClicked((event) -> k.stop());

        send = new Button();
        send.setText("Send");
        send.setOnMouseClicked((event) -> {
            short r;
            double s, d;

            try {
                s = Double.parseDouble(speed.getText());
                r = Short.parseShort(radius.getText());
                d = Double.parseDouble(distance.getText());
            } catch (Exception e) {
                s = 0;
                r = 0;
                d = 0;
            }

            if (menu.getValue().equals("Move"))
                k.move((short) s, r);
            else if (menu.getValue().equals("Rotate"))
                k.rotateOne(s, 1000);
            else if (menu.getValue().equals("Move for Distance"))
                k.move((short) s, r, d);
        });

        buttonBox.getChildren().addAll(send, stop);

        HBox minBox = new HBox();
        HBox maxBox = new HBox();

        min = new TextField("30,60,85");
        max = new TextField("50,200,255");

        Label minLabel = new Label("Min HSV");
        Label maxLabel = new Label("Max HSV");

        minBox.getChildren().addAll(minLabel, min);
        maxBox.getChildren().addAll(maxLabel, max);

        // Camera controls
        HBox cameraBox = new HBox();

        startCamera = new Button("Start Camera");
        stopCamera = new Button("Stop Camera");

        startCamera.setOnMouseClicked(event -> cam.viewCamera());
        stopCamera.setOnMouseClicked(event -> cam.stop());

        cameraBox.getChildren().addAll(startCamera, stopCamera);

        // Task buttons
        HBox taskBox = new HBox();

        Button calibrate = new Button("Calibrate");
        calibrate.setOnMouseClicked(event -> cam.calibrate());

        taskOne = new Button("Task 1");
        taskOne.setOnMouseClicked(event -> {
            k.startTask(1);
        });

        taskTwo = new Button("Task 2");
        taskTwo.setOnMouseClicked(event -> k.startTask(2));

        taskBox.getChildren().addAll(calibrate, taskOne, taskTwo);

        leftBox.getChildren().addAll(menu, speedBox, radiusBox, distBox, buttonBox, minBox, maxBox, cameraBox, taskBox, cameraView);
        rightBox.getChildren().add(faceView);
    }
}
