package it.lorenzoranucci.hangman.workers;

import android.os.AsyncTask;
import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.HashMap;
import java.util.Map;

import it.lorenzoranucci.hangman.threads.BackgroundUpdaterThread;
import it.lorenzoranucci.hangman.workers.Hangman;

/**
 * Created by Lorenzo on 01/07/2015.
 */
public class FaceMaskGeneratorMOG2 extends FaceMaskGenerator {

    private BackgroundSubtractorMOG2 backgroundSubtractorMog2=Video.createBackgroundSubtractorMOG2();
    private Mat faceMask;
    private final int DEFAULT_THRESHOLD=16;
    /**
     * Image used for background replacement
     */
    private Mat background;

    /**
     * Set the threshold used to compute foreground-backgruond difference (detect face's pixels)
     */
    private int threshold = DEFAULT_THRESHOLD;




    /**
     * Detect the face's pixel and return a mat of them.
     * @param currentFrameMat Current captured frame
     * @param faceROI Face region of interest
     * @return Mask of point that belong to the foreground(face)
     */
    @Override
    public Mat getFaceMask(Mat  currentFrameMat, Rect faceROI){
        Mat currentFrameMatRGB = new Mat(currentFrameMat.rows(), currentFrameMat.cols(), CvType.CV_8UC3);
        Imgproc.cvtColor(currentFrameMat, currentFrameMatRGB,Imgproc.COLOR_RGBA2RGB);
        backgroundSubtractorMog2.setVarThreshold(threshold);
        backgroundSubtractorMog2.apply(currentFrameMatRGB,faceMask, 0.0001);
        currentFrameMatRGB.release();
        return faceMask.submat(faceROI);
    }

    @Override
    public Mat getNewBackground() {
//        Mat background=new Mat(new Size(0,0),CvType.CV_8UC3);
//        backgroundSubtractorMog2.getBackgroundImage(background);
//        return (background.size().height==0.0 && background.size().width==0.0)?null:background;
        return background;
    }




    @Override
    public void initBackground(CameraBridgeViewBase.CvCameraViewFrame currentFrame){
        Mat currentFrameMat=currentFrame.rgba();
        Mat backgroundMatRGB = new Mat(currentFrameMat.rows(), currentFrameMat.cols(), CvType.CV_8UC3);
        Imgproc.cvtColor(currentFrameMat, backgroundMatRGB,Imgproc.COLOR_RGBA2RGB);
        if(faceMask==null){
            Size s = currentFrameMat.size();
            this.faceMask= new Mat(s, CvType.CV_8UC1);
        }
        backgroundSubtractorMog2.apply(backgroundMatRGB,faceMask, 1);
        background=currentFrameMat.clone();
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        if(parameters.containsKey("threshold")){
            this.threshold=(int)(((double)DEFAULT_THRESHOLD)/((double)50)*Double.valueOf(parameters.get("threshold")));
        }
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params=new HashMap<>();
        params.put("threshold",String.valueOf(threshold));
        return params;
    }




}



