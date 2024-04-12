package com.genersoft.iot.vmp.opencv.frame.detect;

import com.genersoft.iot.vmp.opencv.frame.bean.ObjectDetectionResult;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_text.FloatVector;
import org.bytedeco.opencv.opencv_text.IntVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_dnn.*;

public class YoloDarkNetDetector extends BaseDetector{
    private final static Logger logger = LoggerFactory.getLogger(YoloDarkNetDetector.class);
    private static final String NAME_PATH = "/home/lany/dev/github/blog_demos/javacv-tutorials/yolo-demo/src/main/resources/model/coco.names";

    /**
     * 置信度门限（超过这个值才认为是可信的推理结果）
     */
    private final float confidenceThreshold = 0.5f;

    private final float nmsThreshold = 0.4f;

    // 输出层
    private final StringVector outNames;

    public YoloDarkNetDetector(){
        super();
        // 输出层
        outNames = net.getUnconnectedOutLayersNames();

        // 检查GPU
        if (getCudaEnabledDeviceCount() > 0) {
            net.setPreferableBackend(opencv_dnn.DNN_BACKEND_CUDA);
            net.setPreferableTarget(opencv_dnn.DNN_TARGET_CUDA);
        }
    }

    @Override
    protected Net readNetFrom(String modelWeight, String cfgPath) {
        return readNetFromDarknet(cfgPath, modelWeight);
    }

    @Override
    protected String modelFile() {
        return null;
    }

    @Override
    protected String modelTextFile() {
        return null;
    }

    @Override
    protected String[] classesName() {
        try {
            return Files.readAllLines(Paths.get(NAME_PATH)).toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int imageWidth() {
        return 608;
    }

    @Override
    protected int imageHeight() {
        return 608;
    }

    //检测时间
    private long detectTimeStart;

    @Override
    public List<ObjectDetectionResult> processDetection(Mat frame) {
        this.detectTimeStart = System.currentTimeMillis();
        MatVector outs = doPredict(frame);
        logger.info("do analyzing");
        return postprocess(frame, outs);
    }

    /**
     * 用神经网络执行推理
     */
    private MatVector doPredict(Mat src) {
        // 将图片转为四维blog，并且对尺寸做调整
        Mat inputBlob = blobFromImage(src,
                1 / 255.0,
                new Size(imageWidth(), imageHeight()),
                new Scalar(0.0),
                true,
                false,
                CV_32F);

        // 神经网络输入
        net.setInput(inputBlob);

        // 设置输出结果保存的容器
        MatVector outs = new MatVector(outNames.size());

        // 推理，结果保存在outs中
        net.forward(outs, outNames);


        // 释放资源
        inputBlob.release();

        return outs;
    }

    /**
     * 推理完成后的操作
     * @param frame
     * @param outs
     * @return
     */
    private List<ObjectDetectionResult> postprocess(Mat frame, MatVector outs) {
        long timeEnd = System.currentTimeMillis();
        final IntVector classIds = new IntVector();
        final FloatVector confidences = new FloatVector();
        final RectVector boxes = new RectVector();

        // 处理神经网络的输出结果
        for (int i = 0; i < outs.size(); ++i) {
            // extract the bounding boxes that have a high enough score
            // and assign their highest confidence class prediction.

            // 每个检测到的物体，都有对应的每种类型的置信度，取最高的那种
            // 例如检车到猫的置信度百分之九十，狗的置信度百分之八十，那就认为是猫
            Mat result = outs.get(i);
            FloatIndexer data = result.createIndexer();

            // 将检测结果看做一个表格，
            // 每一行表示一个物体，
            // 前面四列表示这个物体的坐标，后面的每一列，表示这个物体在某个类别上的置信度，
            // 每行都是从第五列开始遍历，找到最大值以及对应的列号，
            for (int j = 0; j < result.rows(); j++) {
                // minMaxLoc implemented in java because it is 1D
                int maxIndex = -1;
                float maxScore = Float.MIN_VALUE;
                for (int k = 5; k < result.cols(); k++) {
                    float score = data.get(j, k);
                    if (score > maxScore) {
                        maxScore = score;
                        maxIndex = k - 5;
                    }
                }

                // 如果最大值大于之前设定的置信度门限，就表示可以确定是这类物体了，
                // 然后就把这个物体相关的识别信息保存下来，要保存的信息有：类别、置信度、坐标
                if (maxScore > confidenceThreshold) {
                    int centerX = (int) (data.get(j, 0) * frame.cols());
                    int centerY = (int) (data.get(j, 1) * frame.rows());
                    int width = (int) (data.get(j, 2) * frame.cols());
                    int height = (int) (data.get(j, 3) * frame.rows());
                    int left = centerX - width / 2;
                    int top = centerY - height / 2;

                    // 保存类别
                    classIds.push_back(maxIndex);
                    // 保存置信度
                    confidences.push_back(maxScore);
                    // 保存坐标
                    boxes.push_back(new Rect(left, top, width, height));
                }
            }

            // 资源释放
            data.release();
            result.release();
        }

        // remove overlapping bounding boxes with NMS
        IntPointer indices = new IntPointer(confidences.size());
        FloatPointer confidencesPointer = new FloatPointer(confidences.size());
        confidencesPointer.put(confidences.get());

        // 非极大值抑制
        NMSBoxes(boxes, confidencesPointer, confidenceThreshold, nmsThreshold, indices, 1.f, 0);

        // 将检测结果放入BO对象中，便于业务处理
        List<ObjectDetectionResult> detections = new ArrayList<>();
        for (int i = 0; i < indices.limit(); ++i) {
            final int idx = indices.get(i);
            final Rect box = boxes.get(idx);

            final int clsId = classIds.get(idx);

            detections.add(new ObjectDetectionResult(
                    clsId,
                    classNames[clsId],
                    confidences.get(idx),
                    box.x(),
                    box.y(),
                    box.width(),
                    box.height(),
                    detectTimeStart,
                    timeEnd - detectTimeStart
            ));

            // 释放资源
            box.releaseReference();
        }

        // 释放资源
        indices.releaseReference();
        confidencesPointer.releaseReference();
        classIds.releaseReference();
        confidences.releaseReference();
        boxes.releaseReference();

        return detections;
    }
    /**
     * 计算出总耗时
     *
     */
    private double timeUsed() {
        // 总次数
        long totalNums = net.getPerfProfile(new DoublePointer());
        // 频率
        double freq = getTickFrequency() / 1000;
        // 总次数除以频率就是总耗时
        return totalNums / freq;
    }


}
