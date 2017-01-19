package it.lorenzoranucci.hangman.workers;

import android.os.AsyncTask;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.Map;

import it.lorenzoranucci.hangman.threads.BackgroundUpdaterThread;

/**
 * Created by Lorenzo on 01/07/2015.
 */
public class FaceMaskGeneratorCIE1994 extends FaceMaskGenerator implements BackgroundUpdaterThread.BackgroundUpdaterListener {

    private final int DEFAULT_QUALITY=50;
    private final int DEFAULT_THRESHOLD=25;

    /**
     * Image used for background replacement
     */
    private Mat background;

    private final Long BACKGROUND_UPDATE_DELAY = 70000L;

    /**
     * Set the threshold used to compute foreground-backgruond difference (detect face's pixels)
     */
    private int threshold = DEFAULT_THRESHOLD;
    /**
     * The less is the quality the more the frames are resized before to be edited
     */
    private int quality = DEFAULT_QUALITY;

    /**
     * True when TimerTask have to stop
     */
    private boolean isTimerTaskToStop=false;

    /**
     * True when it's time to update background
     */
    private boolean isTimeToUpdateBackground=false;

    /**
     * Thread that update the background
     */
    private BackgroundUpdaterThread backgroundUpdaterThread;


    @Override
    /**
     * Detect the face's pixel and return a mat of them.
     * @param faceROI Face region of interest
     * @return Mask of point that belong to the foreground(face)
     */
    public Mat getFaceMask(Mat lastFrame , Rect faceROI){
        Mat currentMatRGBA=lastFrame.submat(faceROI);
        if(isTimeToUpdateBackground){
            isTimeToUpdateBackground=false;
            backgroundUpdaterThread.setCurrentFrame(lastFrame);
        }
        Mat backgroundMatRGBA=background.submat(faceROI);
        /*resize the image due to the quality user settings*/
        double iRows = ((double) currentMatRGBA.rows() / 100) * quality;
        double iCols = ((double) currentMatRGBA.cols() / 100) * quality;

        Mat currentMatRGBAScaled = new Mat((int) iRows, (int) iCols, currentMatRGBA.type());
        Mat backgroundMatRGBAScaled = new Mat((int) iRows, (int) iCols, backgroundMatRGBA.type());
        Imgproc.resize(currentMatRGBA, currentMatRGBAScaled, new Size(iRows, iCols));
        Imgproc.resize(backgroundMatRGBA, backgroundMatRGBAScaled, new Size(iRows, iCols));

        Mat currentMatRGBScaled = new Mat((int) iRows, (int) iCols,  CvType.CV_8UC3);
        Mat backgroundMatRGBScaled = new Mat((int) iRows, (int) iCols, CvType.CV_8UC3);
        Imgproc.cvtColor(currentMatRGBAScaled, currentMatRGBScaled,Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(backgroundMatRGBAScaled, backgroundMatRGBScaled,Imgproc.COLOR_RGBA2RGB);

        Mat currentMatLABScaled = new Mat((int) iRows, (int) iCols,  CvType.CV_8UC3);
        Mat backgroundMatLABScaled = new Mat((int) iRows, (int) iCols, CvType.CV_8UC3);
        Imgproc.cvtColor(currentMatRGBScaled, currentMatLABScaled,Imgproc.COLOR_RGB2Lab);
        Imgproc.cvtColor(backgroundMatRGBScaled, backgroundMatLABScaled,Imgproc.COLOR_RGB2Lab);


        /*Compute mask in a elliptical region of interest*/
        Mat foreGroundMaskScaled = Mat.zeros(currentMatLABScaled.rows(), currentMatLABScaled.cols(), CvType.CV_8UC1);
        for (int j = 0; j < currentMatLABScaled.rows(); ++j) {
            for (int i = 0; i < currentMatLABScaled.cols(); ++i) {
                double a[] = currentMatLABScaled.get(j, i);
                double b[] = backgroundMatLABScaled.get(j, i);
                double deltaE = getCIE1994(a,b);
                if (deltaE > threshold) {
                    foreGroundMaskScaled.put(j, i, 255);//pixels to draw
                }
            }
        }
        Size originalSize=currentMatRGBA.size();
        Mat foregroundMask = new Mat(originalSize, foreGroundMaskScaled.type());
        Imgproc.resize(foreGroundMaskScaled, foregroundMask, originalSize);
        return foregroundMask;
    }


    @Override
    public Mat getNewBackground() {
        return background;
    }


    @Override
    public  void initBackground(CameraBridgeViewBase.CvCameraViewFrame currentFrame) {
        this.background=currentFrame.rgba().clone();
        start();
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        if(parameters.containsKey("threshold")){
            this.threshold=(int)(((double)DEFAULT_THRESHOLD)/((double)50)*Double.valueOf(parameters.get("threshold")));
        }
        if(parameters.containsKey("quality")){
            this.quality=(int)(((double)DEFAULT_QUALITY)/((double)50)*Double.valueOf(parameters.get("quality")));
            if(quality<10){
                quality=10;
            }
        }
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params=new HashMap<>();
        params.put("quality",String.valueOf(quality));
        params.put("threshold",String.valueOf(threshold));
        return params;
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

        return Math.sqrt((deltaL/(kl*sl))*(deltaL/(kl*sl))     +
                (deltaC/(kc*sc))*(deltaC/(kc*sc))    +
                (deltaH/(kh*sh))*(deltaH/(kh*sh))   );

    }


    /**
     * Stop background update thread and its scheduler
     */
    private void stop() {
        backgroundUpdaterThread.stopThread();
        isTimerTaskToStop=true;
    }

    /**
     * Start background update thread and its scheduler
     */
    private void start() {
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
                if(backgroundUpdaterThread!=null && backgroundUpdaterThread.isAlive()){
                    isTimeToUpdateBackground=true;
                }
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



