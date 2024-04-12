package com.genersoft.iot.vmp.opencv.frame.detect;

import com.genersoft.iot.vmp.opencv.frame.processor.StreamProcessorTask;

public interface IDetection {
    void executeDetection(StreamProcessorTask task);
}
