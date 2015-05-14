package com.company;

import edu.ycp.robotics.KobukiRobot;

import javafx.scene.image.ImageView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class KobukiController {
    private final KobukiRobot k;
    private final ScheduledExecutorService pool;

    private ScheduledFuture currentTask;
    private ScheduledFuture sensorTask;
    private ScheduledFuture taskOne;
    private ScheduledFuture taskTwo;

//    private final int TICK_DURATION = 30; // ms

    private KobukiCamera camera;

    private ImageView faceView;

    private final double TICKS_PER_METER = 11724.4165803;
    private final int MAX_TICKS = 65535;

    KobukiController(String port, ImageView faceView, ImageView cameraView) {
        // Setup thread pool
        // Responsible for movement, input gathering, task execution, and camera
        pool = Executors.newScheduledThreadPool(5);

        // Setup robot
        k = new KobukiRobot(port);
//        k = null;

        // Setup camera
        camera = new KobukiCamera(cameraView, pool);

        // Store reference to ImageView
        this.faceView = faceView;
    }

    public void move(short speed, short radius) {
        // Cancel previous task, if applicable
        stop();

        // Schedule a movement task; packet sent every 42 ms
        currentTask = pool.scheduleAtFixedRate(() -> {
            moveOne(speed, radius);
        }, 0, KobukiRobot.MIN_UPDATE_PERIOD*2, TimeUnit.MILLISECONDS);

        System.out.println("Should start moving!");
    }

    public void move(short speed, short radius, double distance) {
        stop();

        final int startTick = k.getLeftEncoder();

        // Move the Kobuki straight for 1 m
        currentTask = pool.scheduleAtFixedRate(() -> {
            moveOne(speed, radius);

            double tickDiff = (k.getLeftEncoder() + startTick) % MAX_TICKS;

            System.out.println(tickDiff);

            if (tickDiff >= TICKS_PER_METER * distance)
                stop();

        }, 0, KobukiRobot.MIN_UPDATE_PERIOD*2, TimeUnit.MILLISECONDS);
    }

    private void moveOne(short speed, short radius) {
        try {
            k.baseControl(speed, radius);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rotate(short w) {
        // Compute speed and radius for w rad/s
        int speed = (w * 230) / 2;
        short radius = 1;

        move((short)speed, radius);
    }

    public void startTask(int task) {
        stop();

        camera.start(task);

        // Select task
        switch (task) {
            case 1:
                taskOne();
                break;
        }
    }

    public void taskOne() {
        // Run Task 1
        currentTask = pool.scheduleAtFixedRate(() -> {
            if (camera.isVisible())
                System.out.println(camera.getDistance());
        }, 0, KobukiRobot.MIN_UPDATE_PERIOD*2, TimeUnit.MILLISECONDS);
    }

    public KobukiCamera getCamera() {
        return camera;
    }

    public void stop() {
        camera.stop();

        // Cancel running task
        if (currentTask != null && !currentTask.isCancelled())
            currentTask.cancel(true);

        try {
            k.baseControl((short) 0, (short) 0);
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("No need to stop the Kobuki.");
        }

        System.out.println("Stopped!");
    }
}
