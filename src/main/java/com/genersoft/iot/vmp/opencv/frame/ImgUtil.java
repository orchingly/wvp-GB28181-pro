package com.genersoft.iot.vmp.opencv.frame;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

public class ImgUtil {
    public static void saveImage(Mat frame){
        String fileName = "/home/lany/tmp/png/capture-" + System.currentTimeMillis()+".jpg";
        opencv_imgcodecs.imwrite(fileName, frame);
    }
}
