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
    private Mat lastFaceFrame;// last frame with the face
    private Rect lastFaceRect;
    private long facesNotDetectedCounter=10;


    private Point pointWhereToPutTheFace=new Point(0,0);

    List<Mat> backgroundMasks= new ArrayList<Mat>();
    

    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
    }

    public Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        currentFrame= inputFrame.rgba();
        Rect rect;
        if((rect=faceDetect())!=null){//modify currentFrame
            facesNotDetectedCounter=0;
            lastFaceFrame=currentFrame.submat(rect);
            lastFaceRect=rect;
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
        Mat temp=currentFrame;
        mJavaDetector.detectMultiScale(temp, faceDetections);
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





    private void moveFace(){
        if(lastFaceFrame!=null) {
            int width=currentFrame.width();
            int height=currentFrame.height();

            int faceFrameWidth=lastFaceFrame.cols();
            int faceFrameHeight=lastFaceFrame.rows();

            int col=0;
            int row=0;
            if(pointWhereToPutTheFace!=null){
                col=(int)pointWhereToPutTheFace.x-(faceFrameWidth/2);
                row=(int)pointWhereToPutTheFace.y-(faceFrameHeight/2);
            }
            for (int rowFace = 0; rowFace < faceFrameHeight; rowFace=rowFace+1) {
                for (int colFace = 0; colFace < faceFrameWidth; colFace=colFace+1) {
                    int col2=col+colFace;
                    if(col2>0 && row>0 && col2<width && row<height){
                        try {
                            currentFrame.put(row, col2, lastFaceFrame.get(rowFace, colFace));
                        }
                        catch (UnsupportedOperationException e){
                            Log.i("Boia:","PutError");
                        }
                    }
                }
                row++;
            }
        }
    }



    public void setPointWhereToPutTheFace(MotionEvent event, int screenWidth, int screenHeight, float scale){
        double bitmapWidth=frameOnScreen.width()*scale;
        double bitmapHeight=frameOnScreen.height()*scale;
        double borderWidth=(screenWidth-bitmapWidth)/2;
        double borderHeight=(screenHeight-bitmapHeight)/2;
        pointWhereToPutTheFace.x=(event.getX()-borderWidth)/scale;
        pointWhereToPutTheFace.y=(event.getY()-borderHeight)/scale;
    }



}


