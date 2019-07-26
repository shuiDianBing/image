package ImageFeatures;

import org.opencv.core.Core;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * OPENCV特征点java提取与匹配与比较 << https://blog.csdn.net/cnbloger/article/details/77987098
 * Android for OpenCV 边缘角点特征检测<< https://blog.csdn.net/WangRain1/article/details/89553083
 * Android OpenCV学习 << https://www.cnblogs.com/xunzhi/p/9131962.html
 */
public class ExtractSIFT2 {
    public static Mat FeatureSurfBruteforce(Mat src, Mat dst){
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.SURF);
        DescriptorExtractor de = DescriptorExtractor.create(DescriptorExtractor.SURF);
        //DescriptorMatcher Matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        DescriptorMatcher Matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);

        MatOfKeyPoint mkp = new MatOfKeyPoint();
        fd.detect(src, mkp);
        Mat desc = new Mat();
        de.compute(src, mkp, desc);
        Features2d.drawKeypoints(src, mkp, src);


        MatOfKeyPoint mkp2 = new MatOfKeyPoint();
        fd.detect(dst, mkp2);
        Mat desc2 = new Mat();
        de.compute(dst, mkp2, desc2);
        Features2d.drawKeypoints(dst, mkp2, dst);


        // Matching features
        MatOfDMatch Matches = new MatOfDMatch();
        Matcher.match(desc, desc2, Matches);

        double maxDist = Double.MIN_VALUE;
        double minDist = Double.MAX_VALUE;

        DMatch[] mats = Matches.toArray();
        for (int i = 0; i < mats.length; i++) {
            double dist = mats[i].distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        System.out.println("Min Distance:" + minDist);
        System.out.println("Max Distance:" + maxDist);
        List<DMatch> goodMatch = new LinkedList<>();

        for (int i = 0; i < mats.length; i++) {
            double dist = mats[i].distance;
            if (dist < 3 * minDist && dist < 0.2f) {
                goodMatch.add(mats[i]);
            }
        }

        Matches.fromList(goodMatch);
        // Show result
        Mat OutImage = new Mat();
        Features2d.drawMatches(src, mkp, dst, mkp2, Matches, OutImage);

        return OutImage;
    }
    public static Mat FeatureSiftLannbased(Mat src, Mat dst){
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.SIFT);
        DescriptorExtractor de = DescriptorExtractor.create(DescriptorExtractor.SIFT);
        DescriptorMatcher Matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

        MatOfKeyPoint mkp = new MatOfKeyPoint();
        fd.detect(src, mkp);
        Mat desc = new Mat();
        de.compute(src, mkp, desc);
        Features2d.drawKeypoints(src, mkp, src);

        MatOfKeyPoint mkp2 = new MatOfKeyPoint();
        fd.detect(dst, mkp2);
        Mat desc2 = new Mat();
        de.compute(dst, mkp2, desc2);
        Features2d.drawKeypoints(dst, mkp2, dst);


        // Matching features
        MatOfDMatch Matches = new MatOfDMatch();
        Matcher.match(desc, desc2, Matches);

        List<DMatch> l = Matches.toList();
        List<DMatch> goodMatch = new ArrayList<DMatch>();
        for (int i = 0; i < l.size(); i++) {
            DMatch dmatch = l.get(i);
            if (Math.abs(dmatch.queryIdx - dmatch.trainIdx) < 10f) {
                goodMatch.add(dmatch);
            }

        }

        Matches.fromList(goodMatch);
        // Show result
        Mat OutImage = new Mat();
        Features2d.drawMatches(src, mkp, dst, mkp2, Matches, OutImage);

        return OutImage;
    }
    public static Mat FeatureOrbLannbased(Mat src, Mat dst){
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor de = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorMatcher Matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);

        MatOfKeyPoint mkp = new MatOfKeyPoint();
        fd.detect(src, mkp);
        Mat desc = new Mat();
        de.compute(src, mkp, desc);
        Features2d.drawKeypoints(src, mkp, src);

        MatOfKeyPoint mkp2 = new MatOfKeyPoint();
        fd.detect(dst, mkp2);
        Mat desc2 = new Mat();
        de.compute(dst, mkp2, desc2);
        Features2d.drawKeypoints(dst, mkp2, dst);


        // Matching features

        MatOfDMatch Matches = new MatOfDMatch();
        Matcher.match(desc, desc2, Matches);

        double maxDist = Double.MIN_VALUE;
        double minDist = Double.MAX_VALUE;

        DMatch[] mats = Matches.toArray();
        for (int i = 0; i < mats.length; i++) {
            double dist = mats[i].distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        System.out.println("Min Distance:" + minDist);
        System.out.println("Max Distance:" + maxDist);
        List<DMatch> goodMatch = new LinkedList<>();

        for (int i = 0; i < mats.length; i++) {
            double dist = mats[i].distance;
            if (dist < 3 * minDist && dist < 0.2f) {
                goodMatch.add(mats[i]);
            }
        }

        Matches.fromList(goodMatch);
        // Show result
        Mat OutImage = new Mat();
        Features2d.drawMatches(src, mkp, dst, mkp2, Matches, OutImage);

        //Highgui.imwrite("E:/work/qqq/Y4.jpg", OutImage);
        return OutImage;
    }
    public static MatOfRect getFace(Mat src) {
        Mat result = src.clone();
        if (src.cols() > 1000 || src.rows() > 1000) {
            Imgproc.resize(src, result, new Size(src.cols() / 3, src.rows() / 3));
        }

        CascadeClassifier faceDetector = new CascadeClassifier("./resource/haarcascade_frontalface_alt2.xml");
        MatOfRect objDetections = new MatOfRect();
        faceDetector.detectMultiScale(result, objDetections);

        return objDetections;
    }

    //基于opencv-3.4.0的图像特征点提取及图像匹配(Java 版)  << https://blog.csdn.net/u014267900/article/details/79235379
    // 特征点匹配，值越大匹配度越高 (匹配度NaN表示不想管)
    public void imgMatching2(Mat origin,Mat contrast) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //Mat src_base = Imgcodecs.imread("D:\\test\\test5.jpg");
        //Mat src_test = Imgcodecs.imread("D:\\test\\test3.jpg");
        Mat gray_base = new Mat();
        Mat gray_test = new Mat();
        // 转换为灰度
        Imgproc.cvtColor(origin, gray_base, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(contrast, gray_test, Imgproc.COLOR_RGB2GRAY);
        // 初始化ORB检测描述子
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.ORB);//特别提示下这里opencv暂时不支持SIFT、SURF检测方法，这个好像是opencv(windows) java版的一个bug,本人在这里被坑了好久。
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        // 关键点及特征描述矩阵声明
        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint(), keyPoint2 = new MatOfKeyPoint();
        Mat descriptorMat1 = new Mat(), descriptorMat2 = new Mat();
        // 计算ORB特征关键点
        featureDetector.detect(gray_base, keyPoint1);
        featureDetector.detect(gray_test, keyPoint2);
        // 计算ORB特征描述矩阵
        descriptorExtractor.compute(gray_base, keyPoint1, descriptorMat1);
        descriptorExtractor.compute(gray_test, keyPoint2, descriptorMat2);
        float result = 0;
        // 特征点匹配
        System.out.println("test5：" + keyPoint1.size());
        System.out.println("test3：" + keyPoint2.size());
        if (!keyPoint1.size().empty() && !keyPoint2.size().empty()) {
            // FlannBasedMatcher matcher = new FlannBasedMatcher();
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);
            MatOfDMatch matches = new MatOfDMatch();
            matcher.match(descriptorMat1, descriptorMat2, matches);
            // 最优匹配判断
            double minDist = 100;
            DMatch[] dMatchs = matches.toArray();
            int num = 0;
            for (int i = 0; i < dMatchs.length; i++) {
                if (dMatchs[i].distance <= 2 * minDist) {
                    result += dMatchs[i].distance * dMatchs[i].distance;
                    num++;
                }
            }
            // 匹配度计算
            result /= num;
        }
        System.out.println(result);
    }
}
