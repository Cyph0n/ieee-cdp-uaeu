package com.company;

import edu.ycp.robotics.KobukiRobot;

import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class KobukiController {
    private final KobukiRobot k;
    private final ScheduledExecutorService pool;

    private ScheduledFuture currentTask;
    private ScheduledFuture rotateTask;

    private KobukiCamera camera;

    private ImageView faceView;

    // For Task 1 and 2
    private double distance;
    private short speed = 300; // mm/s
    private short radius = 0;
    private double prevError = 99999999;
    private int stopFrames = 0;
    private int startTick;
    private boolean looking = false;
    private boolean found = false;

    private int rotateCount = 0;

    private final short TASK_1_RANGE = 2000; // Distance in mm
    private final short TASK_2_RANGE = 2; // in m

    private final double TICKS_PER_METER = 11724.4165803;
    private final int MAX_TICKS = 65535;

    KobukiController(String port, ImageView faceView, ImageView cameraView) {
        // Setup thread pool
        // Responsible for movement, input gathering, task execution, and camera
        pool = Executors.newScheduledThreadPool(5);

        // Setup robot
        k = new KobukiRobot(port);

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
        // Move the Kobuki straight for some distance (in meters)
        stop();

        final int startTick = k.getLeftEncoder();

        currentTask = pool.scheduleAtFixedRate(() -> {
            moveOne(speed, radius);

            double tickDiff = Math.abs(k.getLeftEncoder() - startTick) % MAX_TICKS;

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

    public void rotate(double w) {
        // Compute speed and radius for w rad/s
        double speed = (w * 230) / 2;

        short radius = 1;

        move((short) speed, radius);
    }

    public void rotateOne(double w, double time) {
        final double speed = (w * 230) / 2;
        final short radius = 1;

        System.out.println(speed);

        rotateTask = pool.scheduleAtFixedRate(() -> {
            if (rotateCount * (KobukiRobot.MIN_UPDATE_PERIOD * 2) >= time) {
                rotateCount = 0;
                stopMovement();
            }

            rotateCount++;

            moveOne((short) Math.round(speed), radius);
        }, 0, KobukiRobot.MIN_UPDATE_PERIOD * 2, TimeUnit.MILLISECONDS);
    }

    public void startTask(int task) {
        stop();

        camera.start(task);

        // Wait for camera to start
        sleep(1000);

        // Select task
        switch (task) {
            case 1:
                taskOne();
                break;
            case 2:
                taskTwo();
                break;
        }
    }

    private void taskOne() {
        currentTask = pool.scheduleAtFixedRate(() -> {
            if (camera.isVisible()) {
                // For end of task
                if (stopFrames > 75) {
                    stopFrames = 0;
                    stopMovement();
                }

                faceView.setImage(new Image("file:images/happy.jpg"));

                updateSpeed();

                moveOne(speed, radius);

                System.out.println("Error = " + prevError + " " + speed + " " + distance);
            }

            else
                stopMovement();
        }, 0, KobukiRobot.MIN_UPDATE_PERIOD*2, TimeUnit.MILLISECONDS);
    }

    private void taskTwo() {
        currentTask = pool.scheduleAtFixedRate(() -> {
            // If the person disappears, start count
            if (!camera.visibleThisFrame && !looking)
                startTick = k.getLeftEncoder();
            else if (camera.visibleThisFrame && !looking)
                startTick = 0;

            if (camera.isVisible() && !looking) {
                // After 3 seconds stopped, assume task ended
                if (stopFrames > 75) {
                    stopFrames = 0;
                    stopMovement();
                }

                updateSpeed();

                moveOne(speed, radius);
            }

            else if (!camera.isVisible() && looking) {
                // Finding the man around corner
                double tickDiff = Math.abs(k.getLeftEncoder() - startTick) % MAX_TICKS;

                System.out.println(distance);

                // If 2 meters done, stop; call other function
                if (tickDiff >= TICKS_PER_METER * (distance-0.2)) {
                    System.out.println("Done");
                    nextTaskTwo();
                }

                moveOne((short)100, (short)0);
            }

            else
                looking = true;
        }, 0, KobukiRobot.MIN_UPDATE_PERIOD*2, TimeUnit.MILLISECONDS);
    }

    private void nextTaskTwo() {
        // Stop currentTask
        stopMovement();

        // Sleep for 0.2 s
        sleep(100);

        // Rotate 90 to the left
        rotateOne(Math.PI, 500); // pi rad/s for 0.5 s

        sleep(2000);

        // If square is visible, move; otherwise rotate -180
        if (camera.isVisible()) {
            double error = camera.getDistance() - TASK_2_RANGE; // error in meters

            move((short)100, (short)0, error);

            found = true;
        }

        else {
            // Rotate 180 deg to the right
            rotateOne(-Math.PI, 1000);
            sleep(2000);

            if (camera.isVisible()) {
                double error = camera.getDistance() - TASK_2_RANGE; // error in meters

                move((short)100, (short)0, error);

                found = true;
            }
        }

        if (found) {
            System.out.println("Found");
            looking = false;
            faceView.setImage(new Image("file:images/happy.jpg"));
        }
    }

    private void updateSpeed() {
        // Slow down or speed up based on distance error
        distance = camera.getDistance() * 1000 - 300; // 30 cm manual offset

        double error = distance - TASK_1_RANGE;

        if (prevError != error) {
            double offset;
            double k;

            if (error >= 0) k = 0.1;
            else k = 0.2;

            // Get the offset
            offset = k * error;

            prevError = error;

            speed += (short)Math.round(offset);
        }

        // Set max and min speed
        if (speed > 400)
            speed = 400;

        if (speed < 0)
            speed = 0;

        if (speed == 0)
            stopFrames++;
        else
            stopFrames = 0;
    }

    public KobukiCamera getCamera() {
        return camera;
    }

    public void stop() {
        camera.stop();

        stopMovement();
    }

    public void stopMovement() {
        // Cancel running task
        if (currentTask != null && !currentTask.isCancelled())
            currentTask.cancel(true);

        if (rotateTask != null && !rotateTask.isCancelled())
            rotateTask.cancel(true);

        try {
            k.baseControl((short) 0, (short) 0);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("No need to stop the Kobuki.");
        }

        speed = 0;
        radius = 0;

        faceView.setImage(null);
    }

    private void sleep(int milli) {
        try {
            Thread.sleep(milli);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
