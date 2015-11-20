# UAEU Bot

UAEU's submission for the IEEE CDP 2015 held at the Petroleum Institute, Abu Dhabi, UAE.

The code in this repository was written completely by myself, Assil Ksiksi, an EE student at UAEU. j-kobuki-2 was written by Dr. Patrick Martin.

## Dependencies

1. OpenCV 3.0 alpha
2. j-kobuki-2
3. jssc

You'll need to add all three of the above to your CLASSPATH, and also set java.library.path to the OpenCV DLL on Windows or .so on Linux/OS X.

## Overview

The IEEE CDP 2015 involved using the Kobuki robot to perform 3 different tasks:

1. Follow a human wearing a [t-shirt with a symbol on the back](http://i.imgur.com/abb8FKH.png) for 5 meters, keeping a distance of 2 meters away from the human during the task. 30 second time limit.

2. The same as above, except the human will turn either left or right after some distance. 45 seconds.

3. Find a human behind a wall in a 5x5 meter area. Stop 1 meter away once found. 5 minute time limit.

## Approach

Since we started quite late, we opted to tackle only the first 2 tasks. Our setup consisted of:

1. Kobuki robot
2. Microsot Lifecam 720p webcam
3. Dell laptop

We did not use any other external sensors, and used only the wheel encoders on the Kobuki to measure travelled distance. Therefore, our entire approach depended on image processing and tracking.

## Image Processing

The code for this part is placed in the KobukiCamera class, and it should work for any webcam.

### Algorithm Outline

1. Get frame from webcam.
2. Convert frame from RGB to HSV space.
3. Convert to binary image (greens as white, rest black).
4. Perform erosion and dilation to lessen noise impact.
5. Find contours in image.
6. Iterate through largest contours checking for rectangles/squares.
7. Return the largest found rectangle/square.
8. Use experimentally derived mathematical fuction to convert square area to distance.
9. Use simple proportional feedback (P controller) to adjust speed depending on returned distance.
