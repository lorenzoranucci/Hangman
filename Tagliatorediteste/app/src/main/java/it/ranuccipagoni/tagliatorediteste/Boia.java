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
    private Mat lastFaceFrame;
    private Rect sourceFaceROI;
    private Rect destFaceROI;
    private Point pointWhereToPutTheFace=new Point(0,0);
    private List<Mat> backgroundsList= new ArrayList<Mat>();
    private int cntBG=0;
    private int cntOldFaceShows=0;



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
            Rect tempSourceFaceROI;
            if(currentFrame!=null &&
                    (tempSourceFaceROI=faceDetect(currentFrame))!=null) {
                sourceFaceROI=tempSourceFaceROI.clone();
                destFaceROI=getDestFaceROI(sourceFaceROI.width, sourceFaceROI.height, currentFrame);
                lastFaceFrame = currentFrame.clone();//clone fa in modo che se cambio currentframe non cambia anche faceframe
                cntOldFaceShows=0;
            }
            if(lastFaceFrame != null
                    && destFaceROI != null
                    && sourceFaceROI != null
                    && backgroundsList != null
                    && ! backgroundsList.isEmpty()
                    && cntOldFaceShows<100){
                Mat currentFrameTemp=currentFrame.clone();
                Mat backgroundFrame=backgroundsList.get(9).clone();
                copyMatToMat(currentFrameTemp, lastFaceFrame, destFaceROI, sourceFaceROI, null);
                copyMatToMat(currentFrameTemp, backgroundFrame, sourceFaceROI, sourceFaceROI, null);
                currentFrame=currentFrameTemp;
                cntOldFaceShows++;
            }
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

    private Mat copyMatToMat(Mat destination, Mat source, Rect destinationROI, Rect sourceROI, Mat mask){
        if (destination !=null
                && source!=null
                && destinationROI.height == sourceROI.height
                && destinationROI.width  == sourceROI.width
                && sourceROI.width<=source.width()
                && sourceROI.height<= source.height()
                && destinationROI.width<=destination.width()
                && destinationROI.height<= destination.height()
                ){
            Mat temp= source.clone().submat(sourceROI);
            if(mask!=null){
                temp.copyTo(destination.submat(destinationROI), mask);
            }
            else{
                temp.copyTo(destination.submat(destinationROI));
            }
        }
        return destination;
    }



    public  void setPointWhereToPutTheFace(MotionEvent event, int screenWidth, int screenHeight, float scale) {
        if (frameOnScreen != null) {
            double bitmapWidth = frameOnScreen.width() * scale;
            double bitmapHeight = frameOnScreen.height() * scale;
            double borderWidth = (screenWidth - bitmapWidth) / 2;
            double borderHeight = (screenHeight - bitmapHeight) / 2;
            pointWhereToPutTheFace.x = (event.getX() - borderWidth) / scale;
            pointWhereToPutTheFace.y = (event.getY() - borderHeight) / scale;
        }
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

   /* public Mat backgroundDifference(Mat image, Mat mask){

    }
*/

}


