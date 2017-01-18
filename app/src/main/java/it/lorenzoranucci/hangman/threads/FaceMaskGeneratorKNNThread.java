package it.lorenzoranucci.hangman.threads;

import android.os.AsyncTask;
import android.view.MotionEvent;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import it.lorenzoranucci.hangman.threads.BackgroundUpdaterThread;

/**
 * Created by Lorenzo on 01/07/2015.
 */
public class FaceMaskGeneratorKNNThread implements BackgroundUpdaterThread.BackgroundUpdaterListener {
    private final Long BACKGROUND_UPDATE_DELAY = 70000L;
    /**
     * It should be a cascade classifier of human faces
     */
    private final CascadeClassifier mJavaDetector;

    /**
     * Last captured frame width
     */
    private Integer frameWidth;
    /**
     * Last captured frame height
     */
    private Integer frameHeight;

    /**
     * Face destination point (where user touched the screen the last time)
     */
    private Point faceDestination = new Point(0, 0);
    /**
     * Image used for background replacement
     */
    private Mat background;
    /**
     * Set the threshold used to compute foreground-backgruond difference (detect face's pixels)
     */
    private int threshold = 16;
    /**
     * The less is the quality the more the frames are resized before to be edited
     */
    private int quality = 10;

    /**
     * True when user want to use the current frame as background
     */
    private boolean isToSetBackground = false;
    /**
     * True when it's time to send a new frame to the background updater thread
     */
    private boolean isCurrentFrameToDeliver = false;

    /**
     * True when TimerTask have to stop
     */
    private boolean isTimerTaskToStop=false;

    /**
     * Thread that update the background
     */
    private BackgroundUpdaterThread backgroundUpdaterThread;


    /**
     * @param mJavaDetector It should be a cascade classifier of human faces
     */
    public FaceMaskGeneratorKNNThread(CascadeClassifier mJavaDetector) {
        this.mJavaDetector = mJavaDetector;
    }


    /**
     * Edit the current camera frame, identifing the presence of a human face and positioning it in the last user touch position.
     *
     * @param inputFrame The last frame captured by the camera
     * @return The edited frame.
     */
    public Mat decapitateWithColorDifference(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrame = inputFrame.rgba();
        /*set background frame if button is pressed*/
        if (isToSetBackground) {
            isToSetBackground = false;
            background = currentFrame.clone();
            backgroundUpdaterThread.setBackground(background);
        }
        frameWidth = currentFrame.width();
        frameHeight = currentFrame.height();
        Rect sourceFaceROI = null;
        Rect destFaceROI = null;
        Mat lastFaceFrame = null;
        if (background != null) {
            /*check if is the time to send the frame to background updater thread*/
            if (isCurrentFrameToDeliver) {
                isCurrentFrameToDeliver = false;
                backgroundUpdaterThread.setCurrentFrame(currentFrame);
            }
            /*check if is there a face in the current frame*/
            Rect tempSourceFaceROI;
            if ((tempSourceFaceROI = faceDetect(currentFrame)) != null) {
                /*compute and position the region of interest of the face*/
                sourceFaceROI = tempSourceFaceROI;
                destFaceROI = getDestFaceROI(sourceFaceROI.width, sourceFaceROI.height, currentFrame);
                lastFaceFrame = new Mat();
                currentFrame.copyTo(lastFaceFrame);
            }
            if (lastFaceFrame != null
                    && destFaceROI != null) {
                Mat faceMask = getFaceMask(currentFrame.submat(sourceFaceROI), background.submat(sourceFaceROI));
                /*replace face region of interest with the old background*/
                copyMatToMat(currentFrame, background, sourceFaceROI, sourceFaceROI, faceMask);
                /*replace face destination region of interest with the captured face*/
                copyMatToMat(currentFrame, lastFaceFrame, destFaceROI, sourceFaceROI, faceMask);

            }
        }
        return currentFrame;
    }


    /**
     * The two image need to be of the same size. Detect the face's pixel and return a mat of them.
     *
     * @param currentFrameFaceROI        Face image
     * @param oldBackgroundEquivalentROI Background image
     * @return Mask of point that belong to the foreground(face)
     */
    private Mat getFaceMask(Mat currentFrameFaceROI, Mat oldBackgroundEquivalentROI) {
        /*resize the image due to the quality user settings*/
        Size s = currentFrameFaceROI.size();
        double iRows = ((double) currentFrameFaceROI.rows() / 100) * quality;
        double iCols = ((double) currentFrameFaceROI.cols() / 100) * quality;

        Mat imageNew = new Mat((int) iRows, (int) iCols, currentFrameFaceROI.type());
        Mat backgroundNew = new Mat((int) iRows, (int) iCols, oldBackgroundEquivalentROI.type());
        Imgproc.resize(currentFrameFaceROI, imageNew, new Size(iRows, iCols));
        Imgproc.resize(oldBackgroundEquivalentROI, backgroundNew, new Size(iRows, iCols));

        Mat imageNew2 = new Mat((int) iRows, (int) iCols,  CvType.CV_8UC3);
        Mat backgroundNew2 = new Mat((int) iRows, (int) iCols, CvType.CV_8UC3);
        Imgproc.cvtColor(imageNew, imageNew2,Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(backgroundNew, backgroundNew2,Imgproc.COLOR_RGBA2RGB);

        Mat imageNew3 = new Mat((int) iRows, (int) iCols,  CvType.CV_8UC3);
        Mat backgroundNew3 = new Mat((int) iRows, (int) iCols, CvType.CV_8UC3);
        Imgproc.cvtColor(imageNew2, imageNew3,Imgproc.COLOR_RGB2Lab);
        Imgproc.cvtColor(backgroundNew2, backgroundNew3,Imgproc.COLOR_RGB2Lab);


        /*Compute mask in a elliptical region of interest*/
        Mat foreGroundMask = Mat.zeros(imageNew3.rows(), imageNew3.cols(), CvType.CV_8UC1);
        double xE = imageNew3.cols() / 2;
        double yE = imageNew3.rows() / 2;
        Ellipse ellipse = new Ellipse(xE, yE, xE, yE);
        for (int j = 0; j < imageNew3.rows(); ++j) {
            for (int i = 0; i < imageNew3.cols(); ++i) {
                if (ellipse.contains(new Point(i, j))) {
                    double a[] = imageNew3.get(j, i);
                    double b[] = backgroundNew3.get(j, i);
                    /*compute euclidean distance*/
                    double dist = getCIE1994(a,b);
                    if (dist > threshold) {
                        foreGroundMask.put(j, i, 255);//pixels to draw
                    }
                }
            }
        }
        Mat f2 = new Mat(s, currentFrameFaceROI.type());
        Imgproc.resize(foreGroundMask, f2, s);
        return f2;
    }


    BackgroundSubtractorMOG2 backgroundSubtractorMog2;
    Mat faceMask;
    public Mat decapitate2(CameraBridgeViewBase.CvCameraViewFrame currentFrame) {
        Mat currentFrameMat = currentFrame.rgba();
//        Mat imageNew = currentFrame.gray();

        Size s = currentFrameMat.size();
    Mat imageNew = new Mat(s, CvType.CV_8UC3);
    Imgproc.cvtColor(currentFrameMat, imageNew ,Imgproc.COLOR_RGBA2RGB);

        frameWidth = currentFrameMat.width();
        frameHeight = currentFrameMat.height();
        Rect sourceFaceROI = null;
        Rect destFaceROI = null;
        Mat lastFaceFrame = null;
        if (isToSetBackground ) {
            isToSetBackground=false;
            background = currentFrameMat.clone();
            getFaceMask2(imageNew,1);
        }
        else if (this.faceMask != null) {
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
                getFaceMask2(imageNew,0);
                /*replace face region of interest with the old background*/
                copyMatToMat(currentFrameMat, background, sourceFaceROI, sourceFaceROI, faceMask.submat(sourceFaceROI));
                /*replace face destination region of interest with the captured face*/
                copyMatToMat(currentFrameMat, lastFaceFrame, destFaceROI, sourceFaceROI, faceMask.submat(sourceFaceROI));

            }
        }
        return currentFrameMat;
    }

    private void getFaceMask2(Mat currentFrame, double learningRate) {
        if(backgroundSubtractorMog2==null){
            backgroundSubtractorMog2= Video.createBackgroundSubtractorMOG2(500,getThreshold(),true);
        }
        if(faceMask==null){
            Size s = currentFrame.size();
            this.faceMask= new Mat(s, CvType.CV_8UC1);
        }
        backgroundSubtractorMog2.setVarThreshold(getThreshold());
        backgroundSubtractorMog2.apply(currentFrame,faceMask, learningRate);

    }

    private double getCIE1994(double a[], double b[]){
        double l1=a[0];
        double l2=b[0];
        double b1=a[1];
        double b2=b[1];
        double a1=a[1];
        double a2=b[1];
        double deltaL=l1-l2;
        double deltaA=a1-a2;
        double deltaB=b1-b2;
        double c1=Math.sqrt(a1*a1+b1*b1);
        double c2=Math.sqrt(a2*a2+b2*b2);
        double deltaC=c1-c2;
        double deltaH=Math.sqrt(deltaA*deltaA+deltaB*deltaB+deltaC*deltaC);
        if(deltaH== Double.NaN){
            deltaH=deltaA*deltaA+deltaB*deltaB+deltaC*deltaC;
        }
        double k1=0.045;
        double k2=0.015;
        double kl=1;
        double kc=1;
        double kh=1;
        double sl=1;
        double sc=1+k1*c1;
        double sh=1+k2*c1;

        double deltaE=Math.sqrt((deltaL/(kl*sl))*(deltaL/(kl*sl))     +
                (deltaC/(kc*sc))*(deltaC/(kc*sc))    +
                (deltaH/(kh*sh))*(deltaH/(kh*sh))   );
        return deltaE;

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

    /**
     * Set the threshold used to compute foreground-backgruond difference (detect face's pixels)
     *
     * @param t value
     */
    public void setThreshold(int t) {
        this.threshold = t * 2;
    }

    /**
     * Set quality.
     * The less is the quality the more the frames are resized before to be edited
     *
     * @param q value
     */
    public void setQuality(int q) {
        this.quality = q + 10;
    }

    /**
     * @return The threshold used to compute foreground-backgruond difference (detect face's pixels)
     */
    public int getThreshold() {
        return this.threshold;
    }

    /**
     * @return /**
     * Get quality.
     * The less is the quality the more the frames are resized before to be edited
     */
    public int getQuality() {
        return this.quality;
    }


    private class Ellipse {
        Point center;
        double semiMajorAxis;
        double semiMinorAxis;

        Ellipse(double centerA, double centerB, double semiMajorAxis, double semiMinorAxis) {
            this.center = new Point(centerA, centerB);
            this.semiMajorAxis = semiMajorAxis;
            this.semiMinorAxis = semiMinorAxis;
        }

        boolean contains(Point point) {
            double distance = (((point.x - center.x) * (point.x - center.x)) / (semiMajorAxis * semiMajorAxis)) +
                    (((point.y - center.y) * (point.y - center.y)) / (semiMinorAxis * semiMinorAxis));
            return distance <= 1;
        }
    }

    public void setBackground() {
        this.isToSetBackground = true;
    }

    public void setNullBackground() {
        this.background = null;
    }


    /**
     * Stop background update thread and its scheduler
     */
    public void stop() {
        backgroundUpdaterThread.stopThread();
        isTimerTaskToStop=true;
    }

    /**
     * Start background update thread and its scheduler
     */
    public void start() {
        if (backgroundUpdaterThread == null) {
            this.backgroundUpdaterThread = new BackgroundUpdaterThread();
            this.backgroundUpdaterThread.setListener(this);
        }
        if (!backgroundUpdaterThread.isAlive()) {
            backgroundUpdaterThread.start();
        }
        new TimerTask().execute(BACKGROUND_UPDATE_DELAY);
    }


    /**
     * Temporize the background updater thread
     */
    private class TimerTask extends AsyncTask<Long, Void, String>{
        /**
         * @param seconds how many seconds to wait
         * @return Operation
         */
        @Override
        protected String doInBackground(Long... seconds) {
            try {
                Thread.sleep(seconds[0], 0);
                return "Send";
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "Relaunch";
            }
        }

        /**
         * Called when it is time to send the current frame to BackgroundUpdaterThread.
         * Re-launch the timer background task.
         * @param operation Send or Relaunch(timer)
         */
        @Override
        protected void onPostExecute(String operation) {
            if(isTimerTaskToStop){
                return;
            }
            else if(operation.equals("Send")){
                isCurrentFrameToDeliver = true;
            }
            new TimerTask().execute(BACKGROUND_UPDATE_DELAY);
        }
    }

    /**
     * Callback for updated background retrieving
     *
     * @param backgroundFrame The updated background
     */
    @Override
    public void onBackgroundUpdated(Mat backgroundFrame) {
        background = backgroundFrame.clone();
    }


}



