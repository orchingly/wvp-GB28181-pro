package com.genersoft.iot.vmp.opencv.frame.bean;

public class MoveAction {
    public MoveAction(long time, boolean move){
        this.actTime = time;
        this.move = move;
    }
    public long actTime;
    public boolean move;
}
