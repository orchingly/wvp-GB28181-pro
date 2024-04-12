package com.genersoft.iot.vmp.opencv.frame.bean;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "视频流检测结果信息")
public class StreamDetectionItem {

    /**移动, 出现*/
    public static final int ACT_START = 1;
    /**停止, 消失*/
    public static final int ACT_STOP = 0;

    @Schema(description = "应用名")
    private String app;
    @Schema(description = "流ID")
    private String stream;
    @Schema(description = "检测类型")
    private int dType;
    @Schema(description = "事件时间")
    private String actTime;

    @Schema(description = "事件类型")
    private int actType;



    public int getActType() {
        return actType;
    }

    public void setActType(int actType) {
        this.actType = actType;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public int getDType() {
        return dType;
    }

    public void setDType(int d_type) {
        this.dType = d_type;
    }

    public String getActTime() {
        return actTime;
    }

    public void setActTime(String act_start) {
        this.actTime = act_start;
    }

    @Override
    public String toString() {
        return "["+app+", "+stream +", "+dType+", "+ actTime +","+actType+"]";
    }
}
