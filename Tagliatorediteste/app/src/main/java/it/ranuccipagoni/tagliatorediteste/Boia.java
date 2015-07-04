package it.ranuccipagoni.tagliatorediteste;

import android.content.Context;
import android.util.Log;

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

    private int cameraViewHeight;
    private int cameraViewWidth;

    private Point pointWhereToPutTheFace;

    List<Mat> backgroundMasks= new ArrayList<Mat>();
    

    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
    }

    public Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frameOnScreen = inputFrame.rgba();

        Mat currentGreyFrame = inputFrame.gray();

        Rect r=riconoscimentoVolto(currentGreyFrame);
        if(r!=null){
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

        //return frameGrey;
        return frameOnScreen;
    }



    private Rect riconoscimentoVolto(Mat image){
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

    private void spostaVolto(Rect r){
        Mat frameVolto=frameOnScreen.submat(r);
        int width=frameOnScreen.width();
        int height=frameOnScreen.height();

        int smallWidth= frameVolto.width();
        int smallHeight= frameVolto.height();


        int rectX= r.x;
        int rectY= r.y;
        int rectWidth= r.width;
        int rectHeight=r.height;


        //if(backgroundMasks.size()>0) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                   /* if(i>rectX && i<rectX+rectWidth && j> rectY && j< rectY+rectHeight ){
                        try {
                            Mat bg = backgroundMasks.get(0);
                            frameOnScreen.put(i, j, bg.get(i, j));
                        }catch (Exception e){

                        }
                    }*/

                //disegno il volto in un angolo
                if (i < smallWidth && j < smallHeight) {
                    try {
                        frameOnScreen.put(i, j, frameVolto.get(i, j));
                    }catch (Exception e){

                    }
                }
            }
        }
        //}
    }






    public void saveCurrentFrameAsBackgroundMask(){
        backgroundMasks.add(this.frameOnScreen);
    }

    public void setCameraViewSize(int width, int height){
        cameraViewHeight=width;
        cameraViewHeight=height;
        double widthD= (double) width/2;
        double heightD=(double) height/2;
        pointWhereToPutTheFace= new Point(widthD,heightD);
        frameOnScreen= new Mat(width,height,CvType.CV_8UC4);
    }
}


/*
* SFONDO
* Point point= new Point(r.x + r.width*0.5, r.y + r.height*0.5 );
                Size size= new Size(r.width,r.height);
                Scalar color= new Scalar(0,255,0);
               // Imgproc.ellipse(image, point, new Size(r.width * 0.5, r.height * 0.5), 0, 0, 360, new Scalar(255, 0, 255), 4, 8, 0);
                //Imgproc.rectangle(image, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(0, 255, 0));
                Mat bgd=new Mat();
                Mat fgd= new Mat();
                Mat mask = new Mat(image.size(),CvType.CV_8U);

                Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
                Imgproc.grabCut(image, mask, r, bgd, fgd, 1,Imgproc.GC_INIT_WITH_MASK);*/