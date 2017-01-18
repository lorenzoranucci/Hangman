package it.lorenzoranucci.hangman.workers;

import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Created by lorenzo on 18/01/17.
 */

public class Hangman {
    public final static int PIXEL_COLOR_BASED=0;
    public final static int MOG2=1;
    public final static int KNN=2;

    public CascadeClassifier getmJavaDetector() {
        return mJavaDetector;
    }

    private FaceMaskGenerator faceMaskGenerator;

    /**
     * It should be a cascade classifier of human faces
     */
    private final CascadeClassifier mJavaDetector;


    /**
     * Face destination point (where user touched the screen the last time)
     */
    private Point faceDestination = new Point(0, 0);
    private Integer frameWidth;
    private Integer frameHeight;


    /**
     * @param mJavaDetector It should be a cascade classifier of human faces
     */
    public Hangman(CascadeClassifier mJavaDetector) {
        this(mJavaDetector,PIXEL_COLOR_BASED);
    }

    /**
     * @param mJavaDetector It should be a cascade classifier of human faces
     */
    public Hangman(CascadeClassifier mJavaDetector, int backgroundSubtractionType) {
        this.mJavaDetector = mJavaDetector;
        setFaceMaskGenerator(backgroundSubtractionType);
    }

    /**
     * Edit the current camera frame, identifing the presence of a human face and positioning it in the last user touch position.
     *
     * @param inputFrame The last frame captured by the camera
     * @return The edited frame.
     */
    public Mat decapitate(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        Mat currentFrameMat = inputFrame.rgba();
        Rect sourceFaceROI = null;
        Rect destFaceROI = null;
        Mat lastFaceFrame = null;
        Mat background= faceMaskGenerator.getNewBackground();
        if (background != null) {
            /*check if is there a face in the current frame*/
            Rect tempSourceFaceROI;
            if ((tempSourceFaceROI = faceDetect(currentFrameMat)) != null) {
                /*compute and position the region of interest of the face*/
                sourceFaceROI = tempSourceFaceROI;
                destFaceROI = getDestFaceROI(sourceFaceROI.width, sourceFaceROI.height, currentFrameMat);
                lastFaceFrame = new Mat();
                currentFrameMat.copyTo(lastFaceFrame);
            }
            if (lastFaceFrame != null
                    && destFaceROI != null) {
                Mat faceMask = faceMaskGenerator.getFaceMask(currentFrameMat ,  sourceFaceROI);
                if(faceMask!=null && faceMask.size().equals(sourceFaceROI.size()) && faceMask.size().equals(destFaceROI.size())){
                    /*replace face region of interest with the old background*/
                    copyMatToMat(currentFrameMat, background, sourceFaceROI, sourceFaceROI, faceMask);
                /*replace face destination region of interest with the captured face*/
                    copyMatToMat(currentFrameMat, lastFaceFrame, destFaceROI, sourceFaceROI, faceMask);
                }
            }
        }
        return currentFrameMat;
    }



    public void initBackground(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        faceMaskGenerator.initBackground(inputFrame);


        Mat currentFrameMat=inputFrame.gray();
        frameWidth=currentFrameMat.width();
        frameHeight=currentFrameMat.height();
    }

    /**
     * Detect face position
     *
     * @param currentFrame The last frame captured by the camera
     * @return Rect that contain the face or null
     */
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
            return incrementROISize(maxRect);
        }
        return null;
    }

    /**
     * Resize the region of interest
     *
     * @param roi A rectangular region of interest
     * @return Resized ROI
     */
    private Rect incrementROISize(Rect roi) {
        double h = roi.height;
        double hdiff = (h / 100) * 90;//90% higher
        double h2 = h + hdiff;

        double w = roi.width;
        double wdiff = (w / 100) * 45;//45% wider
        double w2 = w + wdiff;

        double x = roi.x;
        double x2 = x - (wdiff / 2);
        double y = roi.y;
        double y2 = y - (hdiff * 5 / 7);


        if (x2 < 0) {
            w2 -= Math.abs(x2);
            x2 = 0;
        }
        if (y2 < 0) {
            h2 -= Math.abs(y2);
            y2 = 0;
        }

        double xn = x2 + w2;
        double yn = y2 + h2;

        if (xn > frameWidth) {
            double diff = xn - frameWidth;
            w2 -= diff + 1;
        }
        if (yn > frameHeight) {
            double diff = yn - frameHeight;
            h2 -= diff + 1;
        }

        return new Rect((int) x2, (int) y2, (int) w2, (int) h2);

    }

    /**
     * Print sourceROI of source into destinationROI of destination
     *
     * @param destination    Image to edit
     * @param source         Source image
     * @param destinationROI Region of interest of destination image
     * @param sourceROI      Region of interest of source image
     * @param mask           Matrix mask of point to be drawn
     */
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

    /**
     * Map the point of the user touch with the equivalent point in the image
     *
     * @param event User touch
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     * @param scale Scale property by OpenCv
     */
    public void setFaceDestination(MotionEvent event, int screenWidth, int screenHeight, float scale) {
        if (frameWidth != null && frameHeight != null) {
            double bitmapWidth = frameWidth * scale;
            double bitmapHeight = frameHeight * scale;
            double borderWidth = (screenWidth - bitmapWidth) / 2;
            double borderHeight = (screenHeight - bitmapHeight) / 2;
            faceDestination.x = (event.getX() - borderWidth) / scale;
            faceDestination.y = (event.getY() - borderHeight) / scale;
        }
    }


    /**
     * Return the region of interest of the face destination
     *
     * @param faceWidth Face width
     * @param faceHeight Face heigth
     * @param currentFrame Current frame
     * @return the region of interest of the face destination
     */
    private  Rect getDestFaceROI(int faceWidth, int faceHeight, Mat currentFrame) {
        Rect rect = new Rect(0, 0, faceWidth, faceHeight);
        if (faceDestination != null && currentFrame != null) {
            int x0 = (int) faceDestination.x;
            int y0 = (int) faceDestination.y;
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


    public void setFaceMaskGenerator(int backgroundSubtractionType) {
        switch (backgroundSubtractionType){
            case PIXEL_COLOR_BASED: faceMaskGenerator =new FaceMaskGeneratorPCB();
                break;
//            case MOG2: faceMaskGenerator=new FaceMaskGeneratorMOG2Thread();
//                break;
//            case KNN: faceMaskGenerator=new FaceMaskGeneratorKNNThread();
//                break;
            default: faceMaskGenerator =new FaceMaskGeneratorPCB();
        }
    }

    public FaceMaskGenerator getFaceMaskGenerator() {
        return faceMaskGenerator;
    }

    public void stop(){
    }
}
