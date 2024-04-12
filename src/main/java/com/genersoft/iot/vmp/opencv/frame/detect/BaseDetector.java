package com.genersoft.iot.vmp.opencv.frame.detect;

import com.genersoft.iot.vmp.opencv.frame.bean.ObjectDetectionResult;
import org.aspectj.apache.bcel.generic.RET;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.Net;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe;

public abstract class BaseDetector {
    //分辨率
    private static final int IMG_WIDTH = 300;
    private static final int IMG_HEIGHT = 300;
    private final int width;
    private final int  height;
    private static final float MEAN_VALUE = 127.5f;
    //均值
    private final float meanVal;
    /**缩放因子*/
    private static final float SCALE_FACTOR = 1.0f;

    private static final float THRESHOLD = 0.5f;

    private final float scaleFactor;
    /**目标类别*/
    protected final String[] classNames;
    /**置信度阈值*/
    private final float threshold ;
    //模型文件
    private final String modelFile;
    //二进制描述文件
    private final String modelTextFile;
    protected final Net net;

//    private Executor executor;

    public BaseDetector(){
        this.classNames = classesName();
        this.modelFile = modelFile();
        this.modelTextFile = modelTextFile();
        this.width = imageWidth();
        this.height = imageHeight();
        this.meanVal = meanValue();
        this.scaleFactor = scaleFactor();
        this.threshold = threshold();
        this.net = readNetFrom(modelFile, modelTextFile);
    }

//    public void setExecutor(Executor executor){
//        this.executor = executor;
//    }

//    public Executor getExecutor(){
//        return this.executor;
//    }

    /**
     *
     * @return org.bytedeco.opencv.opencv_dnn.Net
     */
    protected abstract Net readNetFrom(String modelFile, String modelTextFile);

    /**
     *
     * @return 模型文件
     */
    protected abstract String modelFile();

    /**
     *
     * @return 模型描述文件
     */
    protected abstract String modelTextFile();

    /**
     * 训练模型时所需要的宽
     * @return 宽
     */
    protected int imageWidth(){
        return IMG_WIDTH;
    }

    /**
     * 训练模型时所需要的高
     * @return 高
     */
    protected int imageHeight(){
        return IMG_HEIGHT;
    }

    /**
     * 均值
     */
    protected float meanValue(){
        return MEAN_VALUE;
    }

    /**
     * 缩放因子,默认1.0
     * @return 默认值
     */
    protected float scaleFactor(){
        return SCALE_FACTOR;
    }

    /**
     * 不同的算法应当有个合理的阈值,默认0.5
     * @return value
     */
    protected float threshold(){
        return THRESHOLD;
    }
    /**
     *
     * @return 检测类别
     */
    protected abstract String[] classesName();

    protected float confidenceThreshold(){
        return 0.6f;
    }

    /**
     * 对象检测
     * @param frame 图像帧
     */
    public synchronized List<ObjectDetectionResult> processDetection(Mat frame){
        long detectTimeStart = System.currentTimeMillis();
        /*
        blobFromImage(InputArray image,
        double scalefactor=1.0,
		      const Size& size = Size(),
			  const Scalar& mean = Scalar(),
                bool swapRB = false,
                bool crop = false,
        int ddepth = CV_32F)
        image：这个就是我们将要输入神经网络进行处理或者分类的图片。
        mean：需要将图片整体减去的平均值，如果我们需要对RGB图片的三个通道分别减去不同的值，那么可以使用3组平均值，如果只使用一组，那么就默认对三个通道减去一样的值。减去平均值（mean）：为了消除同一场景下不同光照的图片，对我们最终的分类或者神经网络的影响，我们常常对图片的R、G、B通道的像素求一个平均值，然后将每个像素值减去我们的平均值，这样就可以得到像素之间的相对值，就可以排除光照的影响。
        scalefactor：当我们将图片减去平均值之后，还可以对剩下的像素值进行一定的尺度缩放，它的默认值是1，如果希望减去平均像素之后的值，全部缩小一半，那么可以将scalefactor设为1/2。
        size：这个参数是我们神经网络在训练的时候要求输入的图片尺寸。
        swapRB：OpenCV中认为我们的图片通道顺序是BGR，但是我平均值假设的顺序是RGB，所以如果需要交换R和G，那么就要使swapRB=true
        */
        // 预测
        Mat inputblob = blobFromImage(frame,
                scaleFactor,
                new Size(width, height),
                new Scalar(meanVal),
                true,
                false,
                CV_32F);

        net.setInput(inputblob);

        Mat detectionOutMat = net.forward();
        long detectTimeEnd = System.currentTimeMillis();

        // 解析检测结果
        Mat detectionMat = new Mat(detectionOutMat.size(2), detectionOutMat.size(3), CV_32F, detectionOutMat.ptr());
        // 置信度
        float confidence_threshold = confidenceThreshold();

        List<ObjectDetectionResult> detections = new ArrayList<>();

        for (int i = 0; i < detectionMat.rows(); i++) {

            float conf = (float)detectionMat.row(i).col(2).createIndexer().getDouble();
            if (conf > confidence_threshold) {
                int clsId = (int) detectionMat.row(i).col(1).createIndexer().getDouble();
//                System.out.println("col 2 " + conf + ", class " + classNames[(int) detectionMat.row(i).col(1).createIndexer().getDouble()]);
//                System.out.println("col 3 " + detectionMat.row(i).col(3).createIndexer().getDouble());
                int x = (int) (detectionMat.row(i).col(3).createIndexer().getDouble() * frame.cols());
                int y = (int) (detectionMat.row(i).col(4).createIndexer().getDouble() * frame.rows());
                int bx = (int) (detectionMat.row(i).col(5).createIndexer().getDouble() * frame.cols());
                int by = (int) (detectionMat.row(i).col(6).createIndexer().getDouble() * frame.rows());
                detections.add(new ObjectDetectionResult(clsId,
                        classNames[clsId - 1],
                        conf,
                        x,
                        y,
                        bx - x,
                        by - y,
                        detectTimeStart,
                        detectTimeEnd - detectTimeStart
                        ));
            }
        }
        // C++释放资源
        detectionOutMat.releaseReference();
        detectionMat.releaseReference();
        inputblob.releaseReference();
        //frame不可释放, 由外部转换器释放
        return detections;
    }

    /**
     * 主动资源释放
     * C++不受垃圾回收控制
     */
    public void close(){
        net.close();
    }

}
