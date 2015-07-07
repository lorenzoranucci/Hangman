package it.ranuccipagoni.tagliatorediteste;

import android.util.Log;
import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lorenzo on 01/07/2015.
 */

public class Boia {
    private final CascadeClassifier mJavaDetector;

    private Mat frameOnScreen;//frame drawn on the screen
    private Point pointWhereToPutTheFace=new Point(0,0);



    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
    }

    public  Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrame= inputFrame.rgba();
        Rect rect;
        if((rect=faceDetect(currentFrame))!=null) {//modify currentFrame
            Mat faceFrame = currentFrame.submat(rect);
            Rect ROIFace=getDestFaceROI(rect.width, rect.height, currentFrame);
            currentFrame=moveFace(currentFrame,faceFrame,ROIFace);
        }
        frameOnScreen=currentFrame;
        return frameOnScreen;
    }



    private Rect faceDetect(Mat currentFrame){
        MatOfRect faceDetections = new MatOfRect();
        mJavaDetector.detectMultiScale(currentFrame, faceDetections);
        Rect[] rects = faceDetections.toArray();
        Rect maxRect=null;
        if(rects.length>0){
            maxRect= rects[0];
            for (int i = 1; i < rects.length ; i++) {
                if(rects[i].area()> maxRect.area()){
                    maxRect=rects[i];
                }
            }
            return maxRect;
        }
        return null;
    }







    private  Mat moveFace(Mat currentFrame, Mat faceFrame, Rect ROIFace){
         if (faceFrame != null && currentFrame!=null ) {
             if (ROIFace.width < currentFrame.width()
                     && ROIFace.height < currentFrame.height()
                     && ROIFace.x >= 0
                     && ROIFace.y >= 0) {
                 faceFrame.copyTo(currentFrame.submat(ROIFace));
                 return currentFrame;
             }
         }
        return currentFrame;
    }



    public  void setPointWhereToPutTheFace(MotionEvent event, int screenWidth, int screenHeight, float scale){
        double bitmapWidth=frameOnScreen.width()*scale;
        double bitmapHeight=frameOnScreen.height()*scale;
        double borderWidth=(screenWidth-bitmapWidth)/2;
        double borderHeight=(screenHeight-bitmapHeight)/2;
        pointWhereToPutTheFace.x=(event.getX()-borderWidth)/scale;
        pointWhereToPutTheFace.y=(event.getY()-borderHeight)/scale;
    }

    public synchronized Rect getDestFaceROI(int faceWidth,int faceHeight, Mat currentFrame){
        if (pointWhereToPutTheFace != null && currentFrame!=null) {
            int x0 = (int) pointWhereToPutTheFace.x;
            int y0 = (int) pointWhereToPutTheFace.y;
            int currentFrameWidth = currentFrame.cols();
            int currentFrameHeight = currentFrame.rows();
            if (currentFrameWidth > faceWidth && currentFrameHeight > faceHeight) {

                x0 = x0 - (faceWidth / 2);
                y0 = y0 - (faceHeight / 2);

                int xn = x0 + faceWidth;
                int yn = y0 + faceHeight;

                if (x0 < 0) {
                    xn += -x0;
                    x0 = 0;
                } else if (xn > currentFrame.width()) {
                    int diff = xn - currentFrame.width() + 1;
                    xn = xn - diff;
                    x0 = x0 - diff;
                }


                if (y0 < 0) {
                    yn += -y0;
                    y0 = 0;
                } else if (yn > currentFrame.height()) {
                    int diff = yn - currentFrame.height() + 1;
                    yn = yn - diff;
                    y0 = y0 - diff;
                }

                return new Rect(x0, y0, faceWidth, faceHeight);
            }
        }

        return new Rect(0, 0, faceWidth, faceHeight);



    }



}


