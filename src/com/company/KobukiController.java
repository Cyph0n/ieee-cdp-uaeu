package com.company;

import edu.ycp.robotics.KobukiRobot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by IEEE on 5/9/2015.
 */
public class KobukiController {
    private final KobukiRobot k;
    private final ScheduledExecutorService pool;

    private ScheduledFuture moveTask;
    private ScheduledFuture sensorTask;

    private KobukiCamera camera;

    KobukiController(String port) {
        // Setup thread pool
        // Responsible for movement and input gathering
        pool = Executors.newScheduledThreadPool(2);

        // Setup robot
        k = new KobukiRobot(port);

        // Setup camera
        camera = new KobukiCamera();
    }

    public void move(short speed, short radius) {
        // Cancel previous task, if applicable
        if (moveTask != null)
            moveTask.cancel(true);

        // Schedule a movement task; packet sent every 42 ms
        moveTask = pool.scheduleAtFixedRate(() -> {
            try {
                k.baseControl(speed, radius);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, KobukiRobot.MIN_UPDATE_PERIOD*2, TimeUnit.MILLISECONDS);

        System.out.println("Should start moving!");
    }

    public void stop() {
        // Cancel running task
        if (moveTask != null)
            moveTask.cancel(true);

        try {
            k.baseControl((short) 0, (short) 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Stopped!");
    }
}
