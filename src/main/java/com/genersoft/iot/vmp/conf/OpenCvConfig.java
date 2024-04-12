package com.genersoft.iot.vmp.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "opencv", ignoreInvalidFields = true)
@Order(0)
public class OpenCvConfig {

    public OpenCvConfig(){
        System.out.println("");
    }

//    @Value("${opencv.yolo-cfg-path}")
    private String yoloCfgPath;

//    @Value("${opencv.yolo-weights-path}")
    private String yoloWeightsPath;

//    @Value("${opencv.yolo-coconames-path}")
    private String yoloCoconamesPath;

//    @Value("${opencv.yolo-width}")
    private int yoloWidth;

//    @Value("${opencv.yolo-height}")
    private int yoloHeight;

    public void setYoloCfgPath(String yoloCfgPath) {
        this.yoloCfgPath = yoloCfgPath;
    }

    public void setYoloWeightsPath(String yoloWeightsPath) {
        this.yoloWeightsPath = yoloWeightsPath;
    }


    public void setYoloWidth(int yoloWidth) {
        this.yoloWidth = yoloWidth;
    }

    public void setYoloHeight(int yoloHeight) {
        this.yoloHeight = yoloHeight;
    }

    public String getYoloCfgPath() {
        return yoloCfgPath;
    }

    public String getYoloWeightsPath() {
        return yoloWeightsPath;
    }

    public String getYoloCoconamesPath() {
        return yoloCoconamesPath;
    }

    public void setYoloCoconamesPath(String yoloCoconamesPath) {
        this.yoloCoconamesPath = yoloCoconamesPath;
    }

    public int getYoloWidth() {
        return yoloWidth;
    }

    public int getYoloHeight() {
        return yoloHeight;
    }
}
