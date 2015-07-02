package it.ranuccipagoni.tagliatorediteste;

import android.content.Context;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Lorenzo on 01/07/2015.
 */

//qualcosa
public class Boia {
    private Mat frame1;
    private Mat frame2;

    boolean acceso=true;
    


    public Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        frame1=inputFrame.rgba();
        frame2=inputFrame.gray();
        if(acceso){
            return frame1;
        }
        else{
            return frame2;
        }
    }







    public void spostaLaTesta(){

    }
}
