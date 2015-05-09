package com.company;

import org.opencv.core.Core;
import org.opencv.highgui.VideoCapture;

/**
 * Created by IEEE on 5/9/2015.
 */
public class KobukiCamera {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private VideoCapture video;

    KobukiCamera() {
        video = new VideoCapture();
        video.open(0);

        if (!video.isOpened())
            System.err.println("Failed to open capture device!");
        else {
            // Init stuff
        }
    }
}
