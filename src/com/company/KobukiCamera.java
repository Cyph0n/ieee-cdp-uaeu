package com.company;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class KobukiCamera {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private VideoCapture video = null;

    final private ImageView im;

    private ScheduledFuture cameraTask = null;
    final private ScheduledExecutorService pool;

    private double distance = -1;
    private boolean visible = false;

    private int calibFrames;
    private double[] calibAvg;
    private final int calibMax = 100;

    // Cached Mats
    final Mat m = new Mat();

    KobukiCamera(ImageView im, ScheduledExecutorService p) {
        // Take reference to pool
        pool = p;

        // Store ref to ImageView for video display
        this.im = im;
    }

    private boolean setup() {
        stop();

        video = new VideoCapture(1);

        if (video.isOpened())
            return true;
        else
            System.out.println("Failed to start camera!");

        return false;
    }

    public void viewCamera() {
        if (!setup())
            return;

        cameraTask = pool.scheduleAtFixedRate(() -> {
            video.read(m);
            showImage(m);
        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    public void calibrate() {
        if (!setup())
            return;

        calibFrames = 0;
        calibAvg = new double[3];

//        final Mat m = new Mat();

        // Task prints out image values
        cameraTask = pool.scheduleAtFixedRate(() -> {
            if (calibFrames == 50) {
                System.out.println("Done");

                calibAvg[0] /= calibMax;
                calibAvg[1] /= calibMax;
                calibAvg[2] /= calibMax;

                System.out.println("Average color: " + Arrays.toString(calibAvg));

                cameraTask.cancel(true);
            }

            video.read(m);
            showImage(m);

            Mat hsv = new Mat();
            Imgproc.cvtColor(m, hsv, Imgproc.COLOR_RGB2HSV);

            double[] avg = Core.mean(hsv).val;

            calibAvg[0] += avg[0];
            calibAvg[1] += avg[1];
            calibAvg[2] += avg[2];

            System.out.println("H = " + avg[0] + ", S = " + avg[1] + ", V = " + avg[2]);

            calibFrames++;
        }, 0, 33, TimeUnit.MICROSECONDS);;
    }

    public void start(final int task) {
        if (!setup())
            return;

        // Start the camera task
        cameraTask = pool.scheduleAtFixedRate(() -> {
            if (task == 1) {
                // Read a frame
                if (video.read(m)) {
                    // Find the square
                    MatOfPoint s = findSquare(m);

                    // Ensure that it's visible, then compute distance
                    if (s == null)
                        visible = false;
                    else {
                        computeDistance(s);
                        visible = true;
                    }
                }
            }

            else if (task == 2) {}

            else if (task == 3) {}
        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        // Cancel the task
        if (cameraTask != null && !cameraTask.isCancelled())
            cameraTask.cancel(true);

        // Close video stream
        if (video != null && video.isOpened())
            video.release();

        im.setImage(null);
    }

    private MatOfPoint findSquare(Mat m) {
        // HSV
        final Mat hsv = new Mat();
        Imgproc.cvtColor(m, hsv, Imgproc.COLOR_RGB2HSV);

        // Keep only greens
        Mat thresh = new Mat();
        Core.inRange(hsv, new Scalar(35, 100, 0), new Scalar(65, 170, 255), thresh);

        // Morphological open and close for noise removal
        Mat strEl = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, strEl);
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_OPEN, strEl);

        showImage(thresh);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresh.clone(), contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat out = m.clone();

        MatOfPoint2f approx = new MatOfPoint2f();

        // Store found squares
        List<MatOfPoint> results = new ArrayList<>();

        // Track largest contour
        double maxArea = 0;

        for (MatOfPoint contour: contours) {
            double area = Imgproc.contourArea(contour);

            if (area > maxArea)
                maxArea = area;
        }

        for (MatOfPoint contour: contours) {
            // Approximate the polygon
            Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) * 0.01, true);

            // Convert approximation
            MatOfPoint a = new MatOfPoint(approx.toArray());

            // Skip if non-convex and less than 0.7*max area
            if (Imgproc.isContourConvex(a) && Imgproc.contourArea(contour) >= maxArea*0.7) {
                List<Point> points = approx.toList();

                if (points.size() == 4) {
                    // If square, add to results
                    results.add(a);
                }
            }
        }

        // Draw found squares on out
//        for (int i = 0; i < results.size(); i++)
//            Imgproc.drawContours(out, results, i, new Scalar(255, 255, 255), 5);

//        showImage(out);

        if (results.size() == 0)
            return null;

        return results.get(0);
//        return out;
    }

    private void showImage(Mat m) {
        MatOfByte mb = new MatOfByte();

        Imgcodecs.imencode(".bmp", m, mb);
        Image image = new Image(new ByteArrayInputStream(mb.toArray()));

        Platform.runLater(() -> im.setImage(image));
    }

    private void computeDistance(MatOfPoint s) {
        double area = Imgproc.contourArea(s);
        area /= 1000;

        // TODO: Derive area computation relation
//        distance = -0.0003*Math.pow(area, 4) + 0.007*Math.pow(area, 3) - 0.0341*Math.pow(area, 2) + 0.0935*area + 0.0929;
        distance = 1.6275*Math.pow(area, -0.517);

//        System.out.println(area);
    }


    public boolean isVisible() {
        return visible;
    }

    public double getDistance() {
        return distance;
    }
}
