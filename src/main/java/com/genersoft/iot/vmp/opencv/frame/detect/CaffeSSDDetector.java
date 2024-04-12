package com.genersoft.iot.vmp.opencv.frame.detect;

import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.opencv_dnn.Net;

/**
 * 检测效率150ms
 */
public class CaffeSSDDetector extends BaseDetector{

    /**目标类别*/
    private static final String[] classNames = { "background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor" };

    @Override
    protected Net readNetFrom(String modelFile, String modelTextFile) {
        return opencv_dnn.readNetFromCaffe(modelTextFile, modelFile);
    }

    @Override
    protected String modelFile() {
        //模型文件
        return "/navi/ipcamera/wvp-GB28181-pro/src/main/resources/dnn/caffe/VOC0712Plus-300/deploy.caffemodel";
    }

    @Override
    protected String modelTextFile() {
        //二进制描述文件
        return "/navi/ipcamera/wvp-GB28181-pro/src/main/resources/dnn/caffe/VOC0712Plus-300/deploy.prototxt";
    }

    @Override
    protected String[] classesName() {
        return classNames;
    }

    /**
     * 1 检测效果最佳
     * @return 1
     */
    @Override
    protected float scaleFactor() {
        return 1.0f;
    }
}
