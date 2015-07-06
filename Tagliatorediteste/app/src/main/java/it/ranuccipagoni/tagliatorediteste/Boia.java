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


    private Mat frameOnScreen;
    private Mat currentFrame;

    private int cameraViewHeight;
    private int cameraViewWidth;

    private Point pointWhereToPutTheFace=new Point(0,0);

    List<Mat> backgroundMasks= new ArrayList<Mat>();
    

    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
    }

    public synchronized Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //frameOnScreen = inputFrame.rgba();
        currentFrame= inputFrame.rgba();
        Mat currentGreyFrame = inputFrame.gray();
        Rect r=riconoscimentoVolto(currentGreyFrame);
        if(r!=null) {
            //Imgproc.ellipse(image, new Point(r.x + r.width*0.5, r.y + r.height*0.5 ), new Size(r.width * 0.5, r.height * 0.5), 0, 0, 360, new Scalar(255, 0, 255), 4, 8, 0);
            //Imgproc.rectangle(currentGreyFrame, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(0, 255, 0));
            /*Fare grabcut e ottenere sagoma della testa
            *
            * Spostare la forma ottenuta centrandola nel punto desiderato, che può essere:
            *   -un punto prefissato se l'utente non ha ancora toccato lo schermo
            *   -l'ultimo punto toccato dall'utente
            *
            *   */
            spostaVolto(r);
        }
        else{
            frameOnScreen=currentFrame;
        }
        return frameOnScreen;
    }



    private synchronized Rect riconoscimentoVolto(Mat image){
        try {
            MatOfRect faceDetections = new MatOfRect();
            mJavaDetector.detectMultiScale(image, faceDetections);
            System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
            Rect[] rects = faceDetections.toArray();
            Rect maxRect=null;
            if(rects.length>0){
                maxRect= rects[0];
                for (int i = 1; i < rects.length ; i++) {
                    if(rects[i].area()> maxRect.area()){
                        maxRect=rects[i];
                    }
                }
            }
           return maxRect;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }



    private synchronized void spostaVolto(Rect r){


        int width=frameOnScreen.width();
        int height=frameOnScreen.height();
        int rectX= r.x;
        int rectY= r.y;
        int rectWidth= r.width;
        int rectHeight=r.height;


        int cntX=0;
        int cntY=0;
        if(pointWhereToPutTheFace!=null){
            cntX=(int)pointWhereToPutTheFace.x-rectWidth/2;
            cntY=(int)pointWhereToPutTheFace.y-rectHeight/2;
        }
        int limitX=rectX+rectWidth;
        int limitY=rectY+rectHeight;
        for (int x = rectX; x < limitX; x=x+1) {
            cntX=cntX+1;
            int cntY2=cntY;
            for (int y = rectY; y < limitY; y=y+1) {
                cntY2=cntY2+1;
                if(cntX>0 && cntY2>0 && cntX<width && cntY2<height){
                   try {
                       frameOnScreen.put(cntX, cntY2, currentFrame.get(x, y));
                   }
                   catch (UnsupportedOperationException e){
                       Log.e("Boia:","Put");
                   }
                }
            }
        }

    }









    public synchronized void setPointWhereToPutTheFace(MotionEvent event, int screenWidth, int screenHeight){
        double bitmapWidth=frameOnScreen.width()*2.25;
        double bitmapHeight=frameOnScreen.height()*2.25;
        double borderWidth=(screenWidth-bitmapWidth)/2;
        double borderHeight=(screenHeight-bitmapHeight)/2;
        pointWhereToPutTheFace.y=(event.getX()-borderWidth)/2.25;
        pointWhereToPutTheFace.x=(event.getY()-borderHeight)/2.25;
    }



}


