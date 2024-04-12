package com.genersoft.iot.vmp.opencv.frame.processor;

import com.genersoft.iot.vmp.opencv.frame.detect.IDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * RTSP视频流定时抓取
 */
//@Component
public class RtspVideoFrameProcessor {

    private final static Logger logger = LoggerFactory.getLogger(RtspVideoFrameProcessor.class);



    private ThreadPoolExecutor detectExecutor;


    public RtspVideoFrameProcessor() {
    }

    /**
     * 共享线程池
     * @param detectExecutor 处理任务的线程池
     */
    public RtspVideoFrameProcessor(ExecutorService detectExecutor){
        this();
        this.detectExecutor = (ThreadPoolExecutor) detectExecutor;
    }

    /**k:stream, v:task*/
    private HashMap<String, StreamProcessorTask> processorMap = new HashMap<>();

    /**
     *
     * @param streamUrl rtsp url
     * @param stream rtsp stream serial number
     */
    public void startGrabVideo(String streamUrl, String stream){
        logger.info("start stream processor " + stream);
        if (streamUrl == null){
            logger.warn("start failed, Stream url not set, return");
            return ;
        }
        if (processorMap.containsKey(stream)){
            logger.warn("detection already running,  {}", stream);
            return;
        }
        //每个流创建独立的解析任务
        StreamProcessorTask streamTask = new StreamProcessorTask(stream, streamUrl);
        //TODO: Full check
        detectExecutor.submit(streamTask.produce());
        processorMap.put(stream, streamTask);
    }

    public void stop(String stream){
        logger.info("stop stream processor " + stream);
        StreamProcessorTask task = processorMap.remove(stream);
        if (task != null){
            task.cancel();
            detectExecutor.remove(task.produce());
            detectExecutor.purge();
        }
        else {
            logger.warn("stream processor " + stream + " task not found");
        }
    }

    public Map<String, StreamProcessorTask> getProcessorMap(){
        return processorMap;
    }


//    public void startVideo(){
//        capture = new VideoCapture(streamUrl, CAP_FFMPEG);
//            logger.info("start to capture video frame");
//        if (streamUrl == null){
//            logger.warn("start failed, Stream url not set, return");
//            isRunning = false;
//            return;
//        }
//        isRunning = true;
//        captureExecutor.submit(() -> {
//                long startGrab = System.currentTimeMillis();
//                if (streamUrl == null){
//                    logger.warn("start failed, Stream url not set, return");
//                    isRunning = false;
//                    return;
//                }
//                boolean isOpen = capture.open(streamUrl);
//                if (!isOpen){
//                    logger.warn("open stream failed, stream url =  " + streamUrl);
//                    return;
//                }
//                Mat mat = new Mat();
//                int height = 600,width = 800;
//
//                long start = System.currentTimeMillis();
//                long current = start;
//                long last = current;
//                while (isRunning){
//                    current = System.currentTimeMillis();
//                    //一个循环内时间戳没有变化
//                    if ((last == current)){
//                        capture.grab();
//                        continue;
//                    }
//                    last = current;
//                    //1秒读一帧
////                    System.out.println("cut = "+ current + ", start = " + start +", delta = " + (current - start));
//                    if (current - start >= 1000 ){
//                        start = current;
//                        long t = System.currentTimeMillis();
//                        boolean read = capture.read(mat);
//                        logger.info("read frame time spend = " + (System.currentTimeMillis() - t) + ", " + current);
//                        if (!read){
//                            logger.warn("read frame failed");
//                            break;
//                        }
//                        // 重置大小
//                        Mat dst = new Mat();
//                        opencv_imgproc.resize(mat, dst, new Size(width,height));
////                        String fileName = "/home/lany/tmp/png/capture-" + System.currentTimeMillis()+".png";
////                        opencv_imgcodecs.imwrite(fileName, dst);
//                        //如果队列已满,先释放队首元素, 否则内存泄漏
//                        if (matQueue.isAtFullCapacity()){
//                            Mat oldMat = takeMat();
//                            if (oldMat != null ){
//                                logger.warn("release mat queue manually");
//                                oldMat.release();
//                            }
//                        }
//                        matQueue.add(dst);
//                    }
//                    //滑动帧
//                    else {
////                        long t = System.currentTimeMillis();
//                        capture.grab();
////                        System.out.println("grab time = " + (System.currentTimeMillis() - t));
//                    }
//                }
//                stop();
////                System.out.println("grab frame time spend = " + (System.currentTimeMillis() - startGrab));
//            });
//    }


}
