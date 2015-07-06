package it.ranuccipagoni.tagliatorediteste;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lorenzo on 01/07/2015.
 */

public class Boia {
    private final CascadeClassifier mJavaDetector;

    private Mat frameOnScreen;//frame drawn on the screen

    private Mat currentFrame;// temp frame for detect face
    private Mat lastFaceFrame=null;// last frame with the face
    private long facesNotDetectedCounter=10;


    private Point pointWhereToPutTheFace=new Point(0,0);

    List<Mat> backgroundMasks= new ArrayList<Mat>();
    

    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
    }

    public  Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        currentFrame= inputFrame.rgba();
        Mat currentGreyFrame = inputFrame.gray();
        Rect rect;
        if((rect=faceDetect())!=null){//modify currentFrame
            facesNotDetectedCounter=0;
            lastFaceFrame=currentFrame.submat(rect);
            moveFace();
        }else{
            facesNotDetectedCounter++;
            if(facesNotDetectedCounter<10 && lastFaceFrame!=null){
                //draw the previous faceFrame
                moveFace();
            }
        }
        frameOnScreen=currentFrame;
        return frameOnScreen;
    }



    private Rect faceDetect(){
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





    private synchronized void moveFace(){
        if(lastFaceFrame!=null) {
            int width=currentFrame.width();
            int height=currentFrame.height();

            int faceFrameWidth=lastFaceFrame.cols();
            int faceFrameHeight=lastFaceFrame.rows();

            int cntX=0;
            int cntY=0;
            if(pointWhereToPutTheFace!=null){
                cntX=(int)pointWhereToPutTheFace.x-faceFrameWidth/2;
                cntY=(int)pointWhereToPutTheFace.y-faceFrameHeight/2;
            }
            for (int x = 0; x < faceFrameWidth; x=x+1) {
                cntX=cntX+1;
                int cntY2=cntY;
                for (int y = 0; y < faceFrameHeight; y=y+1) {
                    cntY2=cntY2+1;
                    if(cntX>0 && cntY2>0 && cntX<width && cntY2<height){
                        try {
                            currentFrame.put(cntX, cntY2, lastFaceFrame.get(x, y));
                        }
                        catch (UnsupportedOperationException e){
                            Log.i("Boia:","PutError");
                        }
                    }
                }
            }
        }
    }



    public void setPointWhereToPutTheFace(MotionEvent event, int screenWidth, int screenHeight){
        double bitmapWidth=frameOnScreen.width()*2.25;
        double bitmapHeight=frameOnScreen.height()*2.25;
        double borderWidth=(screenWidth-bitmapWidth)/2;
        double borderHeight=(screenHeight-bitmapHeight)/2;
        pointWhereToPutTheFace.y=(event.getX()-borderWidth)/2.25;
        pointWhereToPutTheFace.x=(event.getY()-borderHeight)/2.25;
    }



}


