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
public class Boia implements BackgroundUpdaterThread.BackgroundUpdaterListener{
    private final int BACKGROUND_SKIPPED_FRAME=10;
    private final CascadeClassifier mJavaDetector;

    private Integer frameWidth;
    private Integer frameHeight;

    private Point pointWhereToPutTheFace = new Point(0, 0);
    private Mat background;
    private int threshold = 70;

    private boolean isToSetBackground=false;
    BackgroundUpdaterThread backgroundThread;

    int backgroundUpdateCountdown=BACKGROUND_SKIPPED_FRAME;

    public Boia(CascadeClassifier mJavaDetector) {
        this.mJavaDetector = mJavaDetector;
        this.backgroundThread=new BackgroundUpdaterThread();
        this.backgroundThread.setListener(this);
        this.backgroundThread.setPriority(Thread.MIN_PRIORITY);
        backgroundThread.start();
    }


    public synchronized Mat decapitate(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrame = inputFrame.rgba();
        if(isToSetBackground){
            isToSetBackground=false;
            background=currentFrame.clone();
            backgroundThread.setBackground(background);
        }
        frameWidth = currentFrame.width();
        frameHeight = currentFrame.height();
        Rect sourceFaceROI = null;
        Rect destFaceROI = null;
        Mat lastFaceFrame = null;
        Rect tempSourceFaceROI;
        if(background!=null){
            backgroundUpdateCountdown--;
            if(backgroundUpdateCountdown==0){
                backgroundUpdateCountdown=BACKGROUND_SKIPPED_FRAME;
                backgroundThread.setCurrentFrame(currentFrame);
            }
            if((tempSourceFaceROI = faceDetect(currentFrame)) != null ){
                sourceFaceROI = tempSourceFaceROI;
                sourceFaceROI=incrementROISize(sourceFaceROI);
                destFaceROI = getDestFaceROI(sourceFaceROI.width, sourceFaceROI.height, currentFrame);
                lastFaceFrame = currentFrame.clone();
            }
            if (lastFaceFrame != null
                    && destFaceROI != null){
                Mat backgroundMask = getBackgroundMask(currentFrame.submat(sourceFaceROI), background.submat(sourceFaceROI));
                copyMatToMat(currentFrame, background, sourceFaceROI, sourceFaceROI, backgroundMask);
                copyMatToMat(currentFrame, lastFaceFrame, destFaceROI, sourceFaceROI, backgroundMask);

            }
        }
        return currentFrame;
    }




    private Mat getBackgroundMask(Mat image, Mat background ) {
        Mat diffImage=new Mat();
        Core.absdiff(image, background, diffImage);
        Mat foreGroundMask = Mat.zeros(diffImage.rows(), diffImage.cols(), CvType.CV_8UC1);
        double xE= image.cols()/2;
        double yE= image.rows()/2;
        Ellipse ellipse= new Ellipse(xE,yE,xE,yE);
        for (int j = 0; j < diffImage.rows(); ++j) {
            for (int i = 0; i < diffImage.cols(); ++i) {
                double pix[] = diffImage.get(j, i);
                double dist = (pix[0]*pix[0] + pix[1]*pix[1] + pix[2]*pix[2]);
                dist = Math.sqrt(dist);
                if ( dist> threshold && ellipse.contains(new Point(i,j))) {
                    foreGroundMask.put(j, i, 255);//pixels to draw
                }
            }
        }
        return foreGroundMask;
    }


    private Rect faceDetect(Mat currentFrame) {
        MatOfRect faceDetections = new MatOfRect();
        mJavaDetector.detectMultiScale(currentFrame, faceDetections);
        Rect[] rects = faceDetections.toArray();
        Rect maxRect;
        if (rects.length > 0) {
            maxRect = rects[0];
            for (int i = 1; i < rects.length; i++) {
                if (rects[i].area() > maxRect.area()) {
                    maxRect = rects[i];
                }
            }
            return maxRect;
        }
        return null;
    }

    private void copyMatToMat(Mat destination, Mat source, Rect destinationROI, Rect sourceROI, Mat mask) {
        if (destination != null
                && source != null
                && destinationROI.height == sourceROI.height
                && destinationROI.width == sourceROI.width
                && sourceROI.width <= source.width()
                && sourceROI.height <= source.height()
                && destinationROI.width <= destination.width()
                && destinationROI.height <= destination.height()
                ) {
            Mat tempS = source.clone().submat(sourceROI);

            if (mask != null) {
                tempS.copyTo(destination.submat(destinationROI), mask);
            } else {
                tempS.copyTo(destination.submat(destinationROI));
            }
            tempS.release();
        }
    }


    public void setPointWhereToPutTheFace(MotionEvent event, int screenWidth, int screenHeight, float scale) {
        if (frameWidth != null && frameHeight != null) {
            double bitmapWidth = frameWidth * scale;
            double bitmapHeight = frameHeight * scale;
            double borderWidth = (screenWidth - bitmapWidth) / 2;
            double borderHeight = (screenHeight - bitmapHeight) / 2;
            pointWhereToPutTheFace.x = (event.getX() - borderWidth) / scale;
            pointWhereToPutTheFace.y = (event.getY() - borderHeight) / scale;
        }
    }



    public synchronized Rect getDestFaceROI(int faceWidth, int faceHeight, Mat currentFrame) {
        Rect rect = new Rect(0, 0, faceWidth, faceHeight);
        if (pointWhereToPutTheFace != null && currentFrame != null) {
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
                rect.x = x0;
                rect.y = y0;
            }
        }
        return rect;
    }

    public Rect incrementROISize(Rect roi){
        double h=roi.height;
        double hdiff= (h/100)*75;
        double h2= h+  hdiff;

        double w= roi.width;
        double wdiff= (w/100)*45;
        double w2= w + wdiff;

        double x= roi.x;
        double x2= x-(wdiff/2);
        double y= roi.y;
        double y2= y-(hdiff*6/7);



        if(x2<0 ){
            w2-=Math.abs(x2);
            x2=0;
        }
        if(y2<0 ){
            h2-=Math.abs(y2);
            y2=0;
        }

        double xn= x2+w2;
        double yn= y2+h2;

        if(xn>frameWidth){
            double diff= xn-frameWidth;
            w2-=diff+1;
        }
        if(yn>frameHeight){
            double diff= yn-frameHeight;
            h2-=diff+1;
        }

        return new Rect((int)x2,(int)y2,(int)w2,(int)h2);

    }

    public void setThreshold(int t) {
        this.threshold = t;
    }

    public int getThreshold() {
        return this.threshold;
    }

    @Override
    public void onBackgroundUpdated(Mat backgroundFrame) {
        background=backgroundFrame.clone();
    }

    private class Ellipse{
        Point center;
        double semiMajorAxis;
        double semiMinorAxis;

        Ellipse (double centerA, double centerB, double semiMajorAxis, double semiMinorAxis){
            this.center= new Point(centerA,centerB);
            this.semiMajorAxis=semiMajorAxis;
            this.semiMinorAxis=semiMinorAxis;
        }

        public boolean contains(Point point){
            double distance= (((point.x- center.x)*(point.x- center.x))/(semiMajorAxis*semiMajorAxis))+
                    (((point.y- center.y)*(point.y- center.y))/(semiMinorAxis*semiMinorAxis));
            return distance <= 1;
        }
    }

    public void setBackground(){
        this.isToSetBackground=true;
    }


    public void stopThread(){
        backgroundThread.stopThread();
    }


}


