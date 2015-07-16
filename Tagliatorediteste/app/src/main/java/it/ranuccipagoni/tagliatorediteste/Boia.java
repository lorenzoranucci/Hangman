package it.ranuccipagoni.tagliatorediteste;

import android.util.Log;
import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
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
public class Boia implements BackgroundMaskCalculatorThread.BackgroundMaskChangedListener{
    private final CascadeClassifier mJavaDetector;
    BackgroundMaskCalculatorThread  backgroundThread;

    private Integer frameWidth;
    private Integer frameHeight;

    private Point pointWhereToPutTheFace=new Point(0,0);
    private List<Mat> backgroundsList= new ArrayList<>();
    private int cntBG=0;
    private int cntOldFaceShows=0;

    private int threshold = 100;



    public Boia(CascadeClassifier mJavaDetector){
        this.mJavaDetector=mJavaDetector;
        backgroundThread= new BackgroundMaskCalculatorThread(this);
        backgroundThread.start();
        backgroundThread.setPriority(Thread.MIN_PRIORITY);
    }

    public void finalize(){
        backgroundThread.stop=true;
    }

    public  Mat decapita(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrame= inputFrame.rgba();
        backgroundThread.setLastReceivedFrame(currentFrame);
        frameWidth=currentFrame.width();
        frameHeight=currentFrame.height();
        Rect sourceFaceROI=null;
        Rect destFaceROI=null;
        Mat lastFaceFrame=null;
        if(currentFrame!=null) {
            if (cntBG < 10) {
                cntBG++;
                backgroundsList.add(currentFrame.clone());
            } else {
                Rect tempSourceFaceROI;
                if ((tempSourceFaceROI = faceDetect(currentFrame)) != null) {
                    sourceFaceROI = tempSourceFaceROI;
                    destFaceROI = getDestFaceROI(sourceFaceROI.width, sourceFaceROI.height, currentFrame);
                    lastFaceFrame = currentFrame;
                }
                if (lastFaceFrame != null
                        && destFaceROI != null
                        && sourceFaceROI != null
                        && backgroundsList != null
                        && !backgroundsList.isEmpty()
                        ) {
                    Mat currentFrameTemp = currentFrame.clone();
                    Mat backgroundFrame = backgroundsList.get(9).clone();
                    Mat backgroundMask=getBackgroundMask(lastFaceFrame.submat(sourceFaceROI), backgroundFrame.submat(sourceFaceROI), new Mat());

                    copyMatToMat(currentFrameTemp, lastFaceFrame, destFaceROI, sourceFaceROI, backgroundMask );
                    copyMatToMat(currentFrameTemp, backgroundFrame, sourceFaceROI, sourceFaceROI, backgroundMask);
                    currentFrame = currentFrameTemp;
                }
            }
        }
        return currentFrame;
    }


    private Mat getBackgroundMask(Mat image, Mat background, Mat diffImage){
        Core.absdiff(image, background , diffImage);
        Mat foreGroundMask= Mat.zeros(diffImage.rows(),diffImage.cols(), CvType.CV_8UC1);

        double dist;
        int cnt=0;
        for(int j=0; j<diffImage.rows(); ++j) {
            for (int i = 0; i < diffImage.cols(); ++i) {
                double pix[] = diffImage.get(j, i);

                dist = (pix[0] * pix[0] + pix[1] * pix[1] + pix[2] * pix[2]);
                dist = Math.sqrt(dist);

                if (dist < threshold) {
                    foreGroundMask.put(j, i, 255);
                    cnt++;
                }
            }
        }
        int totPix=diffImage.rows() * diffImage.cols();
        Log.i("Background subtraction","Num tot pix: "+totPix+". PixForeg: "+cnt);
        return foreGroundMask;
    }


    private Rect faceDetect(Mat currentFrame){
        MatOfRect faceDetections = new MatOfRect();
        mJavaDetector.detectMultiScale(currentFrame, faceDetections);
        Rect[] rects = faceDetections.toArray();
        Rect maxRect;
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

    private void copyMatToMat(Mat destination, Mat source, Rect destinationROI, Rect sourceROI, Mat mask){
        if (destination !=null
                && source!=null
                && destinationROI.height == sourceROI.height
                && destinationROI.width  == sourceROI.width
                && sourceROI.width<=source.width()
                && sourceROI.height<= source.height()
                && destinationROI.width<=destination.width()
                && destinationROI.height<= destination.height()
                ){
            Mat tempS= source.clone().submat(sourceROI);

            if(mask!=null){
                tempS.copyTo(destination.submat(destinationROI), mask);
            }
            else{
                tempS.copyTo(destination.submat(destinationROI));
            }
            tempS.release();
        }
    }



    public  void setPointWhereToPutTheFace(MotionEvent event, int screenWidth, int screenHeight, float scale) {
        if (frameWidth != null && frameHeight!= null) {
            double bitmapWidth = frameWidth * scale;
            double bitmapHeight = frameHeight * scale;
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
                    x0 = 0;
                } else if (xn > currentFrame.width()) {
                    int diff = xn - currentFrame.width() + 1;
                    x0 = x0 - diff;
                }
                if (y0 < 0) {
                    y0 = 0;
                } else if (yn > currentFrame.height()) {
                    int diff = yn - currentFrame.height() + 1;
                    y0 = y0 - diff;
                }
                rect.x=x0;
                rect.y=y0;
            }
        }
        return rect;
    }

   public void setThreshold(int t){
       this.threshold=t;
   }

    public int getThreshold(){
        return this.threshold;
    }

    @Override
    public void onBackgroundMaskChanged(Mat backgroundMask) {
        Log.i("BGListener", "Ricevuto");
    }
}


