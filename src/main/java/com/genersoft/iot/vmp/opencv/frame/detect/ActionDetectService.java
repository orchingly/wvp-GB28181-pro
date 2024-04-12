package com.genersoft.iot.vmp.opencv.frame.detect;

import com.genersoft.iot.vmp.conf.OpenCvConfig;
import com.genersoft.iot.vmp.opencv.frame.ImgUtil;
import com.genersoft.iot.vmp.opencv.frame.bean.MoveAction;
import com.genersoft.iot.vmp.opencv.frame.bean.ObjectDetectionResult;
import com.genersoft.iot.vmp.opencv.frame.bean.StreamDetectionItem;
import com.genersoft.iot.vmp.opencv.frame.processor.RtspVideoFrameProcessor;
import com.genersoft.iot.vmp.opencv.frame.processor.StreamProcessorTask;

import com.genersoft.iot.vmp.storager.dao.StreamDetectionMapper;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.opencv.opencv_core.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;


import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;


@Service
public class ActionDetectService {

    private static class TaskDiscardOldestPolicy extends ThreadPoolExecutor.DiscardOldestPolicy {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            logger.warn("rejectedExecution: discard detection for {}, in executor {}",r, e);
            super.rejectedExecution(r, e);
        }
    }

    /**写入数据库的时间格式*/
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private final static Logger logger = LoggerFactory.getLogger(ActionDetectService.class);

    /**全局线程池, 抓帧,检测共用*/
    private static ExecutorService detectExecutor ;

    /**
     * 线程池核心容量, CPU线程的2倍
     * 一路stream消耗2个线程
     */
    private static final int MAX_CORE_PROCESSOR = Runtime.getRuntime().availableProcessors() * 2;

    private final RtspVideoFrameProcessor videoProcessor;

    private final SimpleDateFormat format = new SimpleDateFormat(TIME_PATTERN);

    private final Map<String, BaseDetector> personDetectorMap = new HashMap<>();
    private final Map<String, FrameMoveDetector> actionDetectorMap = new HashMap<>();

    private final Map<String, FutureTask<Integer>> detectionTaskPool = new HashMap<>();

    private final Map<String, Boolean> streamObjectMap = new HashMap<>(MAX_CORE_PROCESSOR / 2);
    private final Map<String, MoveAction> streamActionMap = new HashMap<>(MAX_CORE_PROCESSOR / 2);

    private StreamDetectionMapper detectionMapper;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    public ActionDetectService(OpenCvConfig openCvConfig, StreamDetectionMapper detectionMapper){

        detectExecutor = new ThreadPoolExecutor(MAX_CORE_PROCESSOR, MAX_CORE_PROCESSOR,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(MAX_CORE_PROCESSOR), new TaskDiscardOldestPolicy());

        this.videoProcessor = new RtspVideoFrameProcessor(detectExecutor);

        this.detectionMapper = detectionMapper;
    }

    public void startPersonDetect(String url, String app, String stream){
        logger.info("start detection service");

        if (actionDetectorMap.containsKey(stream)){
            logger.warn("Detection already running {}", stream);
            return;
        }
        //关闭警告log
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        videoProcessor.startGrabVideo(url, stream);
        startDetection(stream);
    }

    public void startDetection(String stream) {
        final StreamProcessorTask processorTask = videoProcessor.getProcessorMap().get(stream);
        // 每路视频流分配一个检测器, 不可多线程共用,
        //人形检测
//        final BaseDetector detector = new MobileNetWithTensorFlowDetector();
        //移动检测
        final FrameMoveDetector actionDetector = new FrameMoveDetector();
        logger.info("new person detector , action detector {}", /*detector,*/ actionDetector);
//        personDetectorMap.put(stream, detector);
        actionDetectorMap.put(stream, actionDetector);

        FutureTask<Integer> task = new FutureTask<>(() -> {
            try {
                //可中断条件
                while (!Thread.currentThread().isInterrupted()) {
//                    logger.info("take frame from blocking queue, " + Thread.currentThread());
                    //视频流中待检测的一帧
                    Mat srcFrame = processorTask.takeFrame();
                    if (srcFrame == null) {
                        logger.warn("null Frame");
                        Thread.yield();
                        continue;
                    }
                    //移动检测
                    if (executeMoveDetection(stream, srcFrame, actionDetector)){
                        //物体检测
//                        executePersonDetection(stream, srcFrame, detector);
                    }
                    //释放当前一帧
                    srcFrame.close();
                    Thread.yield();
                }
            }
            catch (Exception e){
                e.printStackTrace();
                return 1;
            }
            return 0;
        });

        detectionTaskPool.put(stream, task);
        logger.info("start detection task {}", task);
        detectExecutor.submit(task);
    }

    private boolean executeMoveDetection(String stream, Mat frame, FrameMoveDetector detector){
        boolean move = detector.detectMoved(frame);
        long moveTime = System.currentTimeMillis();
        MoveAction beforeMoved = streamActionMap.get(stream);
        if (beforeMoved == null){
            streamActionMap.put(stream, new MoveAction(System.currentTimeMillis(), false));
            return false;
        }

        if (!beforeMoved.move){
            if (move) {
                //开始移动
                logger.info("开始移动");
                saveMoveAction(StreamDetectionItem.ACT_START, stream, FrameMoveDetector.D_TYPE);
            }
        }
        else {
            if (!move){
                logger.info("停止移动");
                saveMoveAction(StreamDetectionItem.ACT_STOP, stream, FrameMoveDetector.D_TYPE);
            }
        }
        streamActionMap.put(stream, new MoveAction(moveTime, move));
        return move;
    }

    private void saveMoveAction(int action, String stream, int dType){
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        StreamDetectionItem item = new StreamDetectionItem();

        long nowMillis = System.currentTimeMillis();
        //服务器拿到的数据流有1-2s延迟, 检测本身也等待了15帧的延迟
        long beforeSecondMillis = nowMillis - 2500;
        Date now = new Date(beforeSecondMillis);

        item.setApp("rtp");
        item.setStream(stream);
        item.setDType(dType);
        String time = format.format(now);
        item.setActTime(time);
        item.setActType(action);
        if (detectionMapper.save(item) > 0){
            dataSourceTransactionManager.commit(transactionStatus);
        }
        else {
            logger.error("保存移动事件失败!, action = {}", item);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
    }


    private void executePersonDetection(String stream, Mat srcFrame, BaseDetector detector){
        List<ObjectDetectionResult> results = detector.processDetection(srcFrame);
        // 检测到的目标分类
        boolean detectObj = false;
        for (ObjectDetectionResult obj : results){
            if (obj.className.equals("person")){
                detectObj = true;
                break;
            }
        }
        long timeUSed = 0;
        if (detectObj){
            timeUSed = results.get(0).timeUsed;
        }

        Boolean preObj = streamObjectMap.get(stream);
        if (preObj != null){
            if (!preObj){
                if (detectObj){
                    ImgUtil.saveImage(srcFrame);
                    logger.info("检测到person分类出现, time used {}", timeUSed);
                    saveMoveAction(StreamDetectionItem.ACT_START, stream, MobileNetWithTensorFlowDetector.D_TYPE);
                }
                else {
                    //未检测到
                }
            }
            else {
                if (!detectObj){
                    logger.info("检测到person分类消失");
                    saveMoveAction(StreamDetectionItem.ACT_STOP, stream, MobileNetWithTensorFlowDetector.D_TYPE);
                }
            }
        }
        else {
            if (detectObj){
                logger.info("检测到person分类出现, time used {}", timeUSed);
                saveMoveAction(StreamDetectionItem.ACT_START, stream, MobileNetWithTensorFlowDetector.D_TYPE);
            }
        }
        streamObjectMap.put(stream, detectObj);
    }

    public synchronized void stopDetect(String stream){
        //先取消检测任务
        FutureTask<Integer> task = detectionTaskPool.remove(stream);
        if (task != null){
            logger.info("finish detection task {}", task);
            task.cancel(true);
        }
        streamObjectMap.remove(stream);
        //然后关闭算法
        BaseDetector detector = personDetectorMap.remove(stream);
        if (detector != null){
            logger.info("close detector {}", detector);
            detector.close();
        }
        //最后停止抓帧
        videoProcessor.stop(stream);
        actionDetectorMap.remove(stream);
    }
}
