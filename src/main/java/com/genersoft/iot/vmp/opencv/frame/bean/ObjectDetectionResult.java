package com.genersoft.iot.vmp.opencv.frame.bean;

/**
 * @author will
 * @email zq2599@gmail.com
 * @date 2021/10/17 5:10 下午
 * @description 存数据的bean
 */
public class ObjectDetectionResult {

    public ObjectDetectionResult(int classId, String className, float confidence, int x, int y, int width, int height, long timeStart, long timeUsed){
        this.classId = classId;
        this.className = className;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.timeStart = timeStart;
        this.timeUsed = timeUsed;
    }
    // 类别索引
    public int classId;
    // 类别名称
    public String className;
    // 置信度
    public float confidence;
    // 物体在照片中的横坐标
    public int x;
    // 物体在照片中的纵坐标
    public int y;
    // 物体宽度
    public int width;
    // 物体高度
    public int height;
    //ms
    public long timeStart;
    //ms
    public long timeUsed;
}
