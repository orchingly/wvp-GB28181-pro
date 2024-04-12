package com.genersoft.iot.vmp.opencv.frame.processor;

import com.genersoft.iot.vmp.opencv.frame.detect.FrameMoveDetector;
import com.genersoft.iot.vmp.opencv.frame.detect.IDetection;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
//import org.opencv.core.Mat;
//import org.opencv.videoio.VideoCapture;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Native;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.bytedeco.opencv.global.opencv_imgproc.findContours;

//import static org.opencv.videoio.Videoio.CAP_FFMPEG;


public class StreamProcessorTask {

    private final static Logger logger = LoggerFactory.getLogger(StreamProcessorTask.class);
    /**每个线程都有自己的流处理*/
    private final FFmpegFrameGrabber grabber;

    private static final int FPS = 15;

    private static final int DURATION = 1000 / FPS;
    private static final int QUEUE_CAPACITY = 3;
    /**帧队列*/
    private final LinkedBlockingQueue<Mat> frameQueue;
    private final LinkedBlockingQueue<Frame> frameQueueCache = new LinkedBlockingQueue<>(3);


    private Mat frameCache ;
    private final Lock frameLock = new ReentrantLock();
    private final Condition cl = frameLock.newCondition();


    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    /** 上一帧的直方图*/
    private Mat prevHist = new Mat();

    private final FutureTask<Integer> task;

    /**
     * 当前任务对应的唯一stream
     */
    private final String stream;

    public StreamProcessorTask(String stream, String streamUrl) {
        this.stream = stream;
        this.grabber = new FFmpegFrameGrabber(streamUrl);
        this.frameQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
//        this.converter = new OpenCVFrameConverter.ToMat();

        grabber.setFormat("rtsp");
//        // 设置要从服务器接受的媒体类型，为空默认支持所有媒体类型，支持的媒体类型：[video，audio，data]
        grabber.setOption("allowed_media_types", "video");
//        /*
//         * rtsp_flags:[filter_src,prefer_tcp,listen]
//         * filter_src:仅接受来自协商对等地址和端口的数据包。
//         * prefer_tcp:如果TCP可用作RTSP RTP传输，请首先尝试使用TCP进行RTP传输。
//         * listen:充当rtsp服务器，监听rtsp连接
//         * rtp传输首选使用tcp传输模式
//         */
////        grabber.setOption("rtsp_flags", "prefer_tcp");
//        // socket网络超时时间
        grabber.setOption("stimeout", "3000000");
//        // 设置RTSP传输协议为tcp传输模式
        grabber.setOption("rtsp_transport", "tcp");
//        grabber.setOption("rtbufsize", "0");

//        // 设置读取的最大数据，单位字节
        grabber.setOption("probesize", "100000");
//// 设置分析的最长时间，单位微秒
        grabber.setOption("analyzeduration", "100000");

//        task = new FutureTask<>(new Callable<Integer>() {
//            @Override
//            public Integer call() throws Exception {
//                long start = System.currentTimeMillis();
//                long current = start;
//                long last = current;
//                boolean grabNextFrame = false;
//                VideoCapture capture = new VideoCapture(streamUrl, CAP_FFMPEG);
//                    logger.info("start to capture video frame");
//                boolean isOpen = capture.open(streamUrl);
//                if (!isOpen){
//                    logger.warn("open stream failed, stream url =  " + streamUrl);
//                    return -1;
//                }
//
//                while (!Thread.currentThread().isInterrupted()){
//                    current = System.currentTimeMillis();
//                    //一个循环内时间戳没有变化
//                    if ((last == current)){
//                        capture.grab();
//                        continue;
//                    }
//                    last = current;
//                    //10 ms per frame
//                    Mat frame = new Mat();
//                    if (current - start >= 10 || grabNextFrame){
//                        grabNextFrame = false;
//                        start = current;
//                        boolean read = capture.read(frame);
//                        if (!read){
//                            grabNextFrame = true;
//                            logger.warn("read frame failed");
//                            continue;
//                        }
//                        logger.info("read frame {}", frame.hashCode());
//                    }
//                    frame.release();
//                }
//
//                return 0;
//            }
//        });

        task = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                long start = System.currentTimeMillis();
                long current = start;
                long last = current;
                boolean grabNextFrame = false;
                grabber.start();
                //可取消
                while (!Thread.currentThread().isInterrupted()){
                    current = System.currentTimeMillis();
                    //一个循环内时间戳没有变化
                    if ((last == current)){
                        try (Frame ignoredFrame = grabber.grabImage()) {
                            Thread.yield();
                            continue;
                        } catch (FFmpegFrameGrabber.Exception ignored) {
                            Thread.yield();
                        }
                        continue;
                    }
                    last = current;
                    //DURATION读一帧, 上一帧读取到无效帧,立即再次读取下一帧
//                    System.out.println("cut = "+ current + ", start = " + start +", delta = " + (current - start));
                    if (current - start >= DURATION || grabNextFrame){
                        grabNextFrame = false;
                        start = current;
//                        long t = System.currentTimeMillis();
//                    boolean read = capture.read(mat);
                        try(Frame frame = grabber.grabImage()) {

//                            logger.info("read frame time spend = " + (System.currentTimeMillis() - t) + ", " + current);
                            if (frame == null){
                                logger.warn("read frame null stop");
                                break;
                            }


                            if (frame.image == null){
                                logger.info("invalid frame image == null skip");
                                grabNextFrame = true;
                                continue;
                            }

                            //队列满,删除队首最老元素
                            if (frameQueue.size() == QUEUE_CAPACITY){
                                logger.warn("frameQueue over max capacity 3");
                                Mat old = frameQueue.poll();
                                if (old != null){
                                    old.close();
                                }
                            }

                            //队列Mat都是经过clone的
                            Mat targetFrame = frameToMat(frame);

                            frameQueue.offer(targetFrame);
//                            logger.info("offer frame queue size = "+frameQueue.size());
                        } catch (FFmpegFrameGrabber.Exception e) {
                            logger.warn("read frame failed, " + e.getCause());
                            break;
                        }
                        finally {
                            Thread.yield();
                        }
                    }
                    //滑动帧
                    else {
                        try(Frame ignoreFrame = grabber.grabImage()) {
                        } catch (FFmpegFrameGrabber.Exception ignored) {
                        }
                        finally {
                            Thread.yield();
                        }
                    }
                }
                return 0;
            }
        });
    }

    public Mat frameToMat(Frame frame){
        //frame 转为opencv的Mat
//        return converter.convert(frame);
        //自动关闭
        try (OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat()){

            return converter.convert(frame).clone();
        }
    }

    /**
     * 计算当前帧和上一帧的相似度
     *
     */
    public double compareHistWithLastFrame(Mat src) {
//        long t1 = System.currentTimeMillis();
        Mat hsv = new Mat();
        //图片转HSV
        opencv_imgproc.cvtColor(src, hsv,opencv_imgproc.COLOR_BGR2HSV);

        Mat hist = new Mat();
        int[] channels = {0};
        int[] histSize = {256};
        float[] range = {0,256};
        Mat vMat = new Mat();
        Mat nMat = new Mat();
        //直方图计算
        opencv_imgproc.calcHist(hsv, 1, channels, vMat, hist, 1, histSize, range,true, false);
        //图片归一化
        opencv_core.normalize(hist, hist, 1, hist.rows() , opencv_core.NORM_MINMAX, -1, nMat );



//        opencv_core.absdiff();
//        opencv_imgproc.findContours();



        //上一帧空,仅缓存,不做匹配
        if (prevHist.empty()){
            prevHist = hist;
            return -1;
        }
        //直方图比较相似度
        double similarity = opencv_imgproc.compareHist(prevHist,hist,opencv_imgproc.CV_COMP_CORREL);
        prevHist.close();
        hsv.close();
        vMat.close();
        nMat.close();
        //更新为当前帧的直方图结果
        prevHist = hist;
//        System.out.println("越接近1越相识度越高,比较结果："+similarity + " time used " + (System.currentTimeMillis() - t1));

        return similarity;
    }

    public FutureTask<Integer> produce(){
        return task;
    }

    public void cancel(){
        try {
            //#
            //# A fatal error has been detected by the Java Runtime Environment:
            //#
            //#  SIGSEGV (0xb) at pc=0x00007f85324c220d, pid=699697, tid=0x00007f84bc5e8640
            //#
            //# JRE version: Java(TM) SE Runtime Environment (8.0_202-b08) (build 1.8.0_202-b08)
            //# Java VM: Java HotSpot(TM) 64-Bit Server VM (25.202-b08 mixed mode linux-amd64 compressed oops)
            //# Problematic frame:
            //# C  [libc.so.6+0xc220d]  __memmove_avx_unaligned_erms_rtm+0x3cd
            //#
            //# If you would like to submit a bug report, please visit:
            //#   http://bugreport.java.com/bugreport/crash.jsp
            //# The crash happened outside the Java Virtual Machine in native code.
            //# See problematic frame for where to report the bug.
            //#  Stack: [0x00007f84bc4e9000,0x00007f84bc5e9000],  sp=0x00007f84bc5e70b8,  free space=1016k
            //  Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
            //  C  [libc.so.6+0xc220d]  __memmove_avx_unaligned_erms_rtm+0x3cd
            //  C  [libopencv_core.so.407+0x1ab7b2]  cv::Mat::clone() const+0x32
            //
            //  Java frames: (J=compiled Java code, j=interpreted, Vv=VM code)
            //  J 10081  org.bytedeco.opencv.opencv_core.Mat.clone()Lorg/bytedeco/opencv/opencv_core/Mat; (0 bytes) @ 0x00007f8511818eb8 [0x00007f8511818e80+0x38]
            //  J 10314 C1 com.genersoft.iot.vmp.opencv.frame.processor.StreamProcessorTask.frameToMat(Lorg/bytedeco/javacv/Frame;)Lorg/bytedeco/opencv/opencv_core/Mat; (41 bytes) @ 0x00007f85118ac644 [0x00007f85118ac340+0x30
            //  j  com.genersoft.iot.vmp.opencv.frame.processor.StreamProcessorTask$1.call()Ljava/lang/Integer;+251
            //  j  com.genersoft.iot.vmp.opencv.frame.processor.StreamProcessorTask$1.call()Ljava/lang/Object;+1
            //  J 7011 C1 java.util.concurrent.FutureTask.run()V (126 bytes) @ 0x00007f850fde6e5c [0x00007f850fde6c00+0x25c]
            //  j  java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;+4
            //  J 7011 C1 java.util.concurrent.FutureTask.run()V (126 bytes) @ 0x00007f850fde6e5c [0x00007f850fde6c00+0x25c]
            //  J 7092 C1 java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V (225 bytes) @ 0x00007f8510565114 [0x00007f85105640e0+0x1034]
            //  J 7087 C1 java.util.concurrent.ThreadPoolExecutor$Worker.run()V (9 bytes) @ 0x00007f850fe7f4cc [0x00007f850fe7f3c0+0x10c]
            //  J 7085 C1 java.lang.Thread.run()V (17 bytes) @ 0x00007f850fdf604c [0x00007f850fdf5f00+0x14c]
            //  v  ~StubRoutines::call_stub
            //先取消任务
            task.cancel(true);
            //再停止抓帧
            grabber.stop();
            prevHist.release();
//            converter.close();
            frameQueue.forEach(Mat::close);
            frameQueue.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 如果队列为空则阻塞
     * @return frame
     */
    public Mat takeFrame() throws InterruptedException {
        return frameQueue.take();
    }



    public String getStream() {
        return stream;
    }
}
