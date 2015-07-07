package it.ranuccipagoni.tagliatorediteste;

import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
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
    private List<Mat> backgroundsList= new ArrayList<Mat>();
    private int cntBG=0;


    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
    }

    public  Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrame= inputFrame.rgba();
        if(cntBG<10){
            cntBG++;
            backgroundsList.add(currentFrame.clone());
        }
        else{
            Rect sourceFaceROI;
            if((sourceFaceROI=faceDetect(currentFrame))!=null) {//modify currentFrame
                Mat faceFrame = new Mat(currentFrame,sourceFaceROI);
                Rect destFaceROI=getDestFaceROI(sourceFaceROI.width, sourceFaceROI.height, currentFrame);
                moveFace(currentFrame, faceFrame, destFaceROI);
                Mat backgroundFrame=backgroundsList.get(9).clone();
                replaceFaceWith(currentFrame,backgroundFrame,sourceFaceROI);
                backgroundFrame.release();
            }
        }
        frameOnScreen=currentFrame.clone();
        currentFrame.release();
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







    private  Mat moveFace(Mat currentFrame, Mat faceFrame,  Rect destFaceROI){
         if (faceFrame != null
                 && currentFrame!=null
                 && destFaceROI !=null
                 && backgroundsList!=null && !backgroundsList.isEmpty()) {
             if (destFaceROI.width < currentFrame.width()
                     && destFaceROI.height < currentFrame.height()
                     && destFaceROI.x >= 0
                     && destFaceROI.y >= 0) {
                 faceFrame.copyTo(currentFrame.submat(destFaceROI));
             }
         }
        return currentFrame;
    }

    private Mat replaceFaceWith(Mat currentFrame, Mat replaceFrame, Rect sourceFaceROI){
        if(currentFrame!=null
                && replaceFrame!=null
                && sourceFaceROI!=null){
            if(sourceFaceROI.width <= replaceFrame.width() && sourceFaceROI.height<= replaceFrame.height()){
                Mat backgroundFrame= replaceFrame.submat(sourceFaceROI);
                backgroundFrame.copyTo(currentFrame.submat(sourceFaceROI));
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
        Rect rect= new Rect(0, 0, faceWidth, faceHeight);
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
                rect.x=x0;
                rect.y=y0;
            }
        }
        return rect;
    }



}


