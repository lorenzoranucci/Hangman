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

/**
 * Created by Lorenzo on 01/07/2015.
 */

//qualcosa
public class Boia {
    private Mat frameRGB;
    private Mat frameGrey;
    private final CascadeClassifier mJavaDetector;
    private Long faceNotFoundCounter= new Long(0);
    

    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
    }

    public Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrameRGB = inputFrame.rgba();
        Mat currentFrameGrey = inputFrame.gray();
        Rect r=riconoscimentoVolto(currentFrameGrey);
        if(r!=null){
            Imgproc.rectangle(currentFrameGrey, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(0, 255, 0));
            frameGrey=currentFrameGrey;
        }
        else{
            Log.i("Boia:FaceDetection","Volto non trovato");
            faceNotFoundCounter++;
        }
        return frameGrey;



    }

    private Rect riconoscimentoVolto(Mat image){
        try {
            MatOfRect faceDetections = new MatOfRect();
            mJavaDetector.detectMultiScale(image, faceDetections);
            System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
            Rect[] recta = faceDetections.toArray();
            Rect r = recta[0];
            return r;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
        //Imgproc.ellipse(image, new Point(r.x + r.width*0.5, r.y + r.height*0.5 ), new Size(r.width * 0.5, r.height * 0.5), 0, 0, 360, new Scalar(255, 0, 255), 4, 8, 0);

    }





    public void spostaLaTesta(){

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