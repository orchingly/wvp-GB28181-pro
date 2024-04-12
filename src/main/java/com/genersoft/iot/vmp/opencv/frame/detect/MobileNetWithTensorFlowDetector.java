package com.genersoft.iot.vmp.opencv.frame.detect;

import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * MobileNet SSD预测模型
 * 检测效率30-40ms, 准确度略差
 */
public class MobileNetWithTensorFlowDetector extends BaseDetector{

    public static final int D_TYPE = 2;
    private static final String CLASS_FILE = "dnn/mobileNet/object_detection_classes_coco.txt";

    @Override
    protected Net readNetFrom(String modelFile, String modelTextFile) {
        return opencv_dnn.readNetFromTensorflow(modelFile, modelTextFile);
    }

    @Override
    protected String modelFile() {
        ClassPathResource classPathResource = new ClassPathResource("dnn/mobileNet/frozen_inference_graph.pb");
        return getAbsPath(classPathResource);
    }

    @Override
    protected String modelTextFile() {
        ClassPathResource classPathResource = new ClassPathResource("dnn/mobileNet/ssd_mobilenet_v2_coco_2018_03_29.pbtxt");
        return getAbsPath(classPathResource);
    }

    private String getAbsPath(ClassPathResource resource){
        try {
            String absPath = resource.getFile().getAbsolutePath();
            System.out.println("getAbsPath path = " + absPath);
            return absPath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String[] classesName() {
        return readClasses();
    }

    private String[] readClasses(){
        try {
            ClassPathResource res = new ClassPathResource(CLASS_FILE);
            return Files.readAllLines(res.getFile().toPath()).toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected float confidenceThreshold() {
        return 0.5f;
    }
}
