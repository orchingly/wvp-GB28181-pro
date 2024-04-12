package com.genersoft.iot.vmp.opencv.frame.detect;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Size;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.bytedeco.opencv.global.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.*;


public class FrameMoveDetector {


    private static final int MOVE_FREQ = 15;
    public static final int D_TYPE = 1;

    private Mat preFrame = new Mat();

    private int frameChangeCount = 0;

    /** 1: 移动, 0 静止*/
    private int actionStateMove = 0;

    /**
     * 帧差法轮廓检测
     * @param frame 帧
     * @return 轮廓数量
     */
    public int detectContours(Mat frame){
        Mat grayFrame = frame.clone();
        opencv_imgproc.cvtColor(grayFrame, grayFrame,opencv_imgproc.COLOR_BGR2GRAY);
        if (preFrame.empty()){
            preFrame = grayFrame;
            return 0;
        }

        Mat diff = new Mat();
        opencv_core.absdiff(preFrame, grayFrame, diff);
        opencv_imgproc.threshold(diff, diff, 25, 255, CV_THRESH_BINARY);

        Mat element = opencv_imgproc.getStructuringElement(MORPH_RECT, new Size(3, 3));
        opencv_imgproc.erode(diff, diff, element);

        Mat element2 = opencv_imgproc.getStructuringElement(MORPH_RECT, new Size(20, 20));
        opencv_imgproc.dilate(diff, diff, element2);

        MatVector contours = new MatVector();

        Mat mat1 = new Mat();
        opencv_imgproc.findContours(diff, contours, mat1, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE, new Point(0, 0));
        int contourSize = (int) contours.size();
        //存储当前帧为下一次备用
        preFrame.close();
        preFrame = grayFrame;
        diff.close();

        element.close();
        element2.close();
        contours.close();
        mat1.close();

        return contourSize;
    }

    /**
     * 慢启动,快静止
     * @param frame 内部不会修改
     * @return true 移动
     */
    public boolean detectMoved(Mat frame){
        int contours = detectContours(frame);
        //轮廓数量大于0则检测到移动物体
        final boolean changed = contours > 0;

        //慢启动,快静止
        //灵敏度控制
        //连续大于MOVE_FREQ帧发生移动,才认为是真正的移动
        if (changed){
            //超过两帧发生移动才算移动
            //规避视频上时间戳1s切换一次
            //防止计数器无限增长
            if (frameChangeCount < MOVE_FREQ){
                frameChangeCount ++;
            }
            //达到移动条件
            if (frameChangeCount >= MOVE_FREQ){
                actionStateMove = 1;
            }
        }
        else {
            //条件重置
            //连续MOVE_FREQ帧无变化,则认为停止移动
            if (frameChangeCount > 0){
                frameChangeCount --;
            }
            //经验值, 观察发现视频静止时有花屏跳动的问题,出现波动变化15-14-13-15-14-15-14误认为在移动
            //变化累积减少3帧,重置为无变化,不需要等到累积减少到0, 效果会好些
            if (frameChangeCount <= MOVE_FREQ - 3){
                frameChangeCount = 0;
            }
            //大于0则还存在移动的可能,等于0则达到停止移动阈值
            if (frameChangeCount == 0){
                actionStateMove = 0;
            }
        }

//        System.out.println("frameChangeCount = " + frameChangeCount);

        return actionStateMove == 1 ;
    }

    public void close(){
        preFrame.close();
    }
}
