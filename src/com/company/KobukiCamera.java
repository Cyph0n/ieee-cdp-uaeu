package com.company;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextField;

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

    private double distance = 1;
    private boolean visible = true;
    private int notVisibleFrames = 0;
    public boolean visibleThisFrame = true;

    private int calibFrames;
    private double[] calibAvg;
    private final int calibMax = 100;

    public TextField min;
    public TextField max;

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

        video = new VideoCapture();
        video.open(1);

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
            findSquare(m);
        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    public void calibrate() {
        if (!setup())
            return;

        calibFrames = 0;
        calibAvg = new double[3];

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

        if (task == 1)
            cameraTask = pool.scheduleAtFixedRate(() -> {
                // Read a frame
                if (video.read(m)) {
                    // Find the square
                    MatOfPoint s = findSquare(m);

                    // Ensure that it's visible for at least 30 frames, then compute distance
                    if (s == null) {
                        if (notVisibleFrames <= 90)
                            notVisibleFrames++;
                        else
                            visible = false;
                    }
                    else {
                        computeDistance(s);
                        visible = true;
                        notVisibleFrames = 0;
                    }
                }
            }, 0, 33, TimeUnit.MILLISECONDS);
        else if (task == 2)
            cameraTask = pool.scheduleAtFixedRate(() -> {
                // Read a frame
                if (video.read(m)) {
                    // Find the square
                    MatOfPoint s = findSquare(m);

                    // Ensure that it's visible, then compute distance
                    if (s == null) {
                        visibleThisFrame = false;
                        if (notVisibleFrames <= 90)
                            notVisibleFrames++;
                        else
                            visible = false;
                    }

                    else {
                        visibleThisFrame = true;
                        computeDistance(s);
                        visible = true;
                    }
                }
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

        Scalar minGreen = makeScalar(min);
        Scalar maxGreen = makeScalar(max);

        Core.inRange(hsv, minGreen, maxGreen, thresh);

        // Morphological open and close for noise removal
        Mat strEl = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, strEl);
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_OPEN, strEl);

        showImage(thresh);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresh.clone(), contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

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
            if (Imgproc.isContourConvex(a) && Imgproc.contourArea(contour) >= maxArea) {
                List<Point> points = approx.toList();

                if (points.size() == 4) {
                    // If square, add to results
                    results.add(a);
                }
            }
        }

        if (results.size() == 0)
            return null;

        return results.get(0);
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

        // Derived area-distance relation for 10x10 cm square
        distance = 1.6275*Math.pow(area, -0.517);
    }

    private static Scalar makeScalar(TextField t) {
        int[] v = new int[3];
        int i;

        try {
            String[] s = t.getText().split(",");

            for (i = 0; i < 3; i++)
                v[i] = Integer.parseInt(s[i]);

            return new Scalar(v[0], v[1], v[2]);
        } catch (Exception e) {
            System.out.println("Parsing error for makeScalar!");
            return new Scalar(0, 0, 0);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public double getDistance() {
        return distance;
    }
}
