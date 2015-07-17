package it.ranuccipagoni.tagliatorediteste;


import org.opencv.core.Mat;


/**
 * Created by Lorenzo on 16/07/2015.
 */

public class BackgroundMaskCalculatorThread extends Thread {
    BackgroundMaskChangedListener listener;
    boolean stop=false;
    private Mat lastReceivedFrame=null;
    private Mat backgroundMask;

    BackgroundMaskCalculatorThread(BackgroundMaskChangedListener listener){
        this.listener=listener;

    }
    @Override
    public void run() {
       /* while (listener!=null && !stop){
            if(lastReceivedFrame!=null){
                synchronized (lastReceivedFrame){
                    Mat frame= lastReceivedFrame;
                    lastReceivedFrame=null;
                    //calcola
                    for(int i=0; i<frame.cols();i++){
                        for(int j=0; j<frame.rows();j++){

                        }
                    }
                    Log.i("BGThread","Calcolato...");
                }
            }
            listener.onBackgroundMaskChanged(backgroundMask);
        }*/
    }
    
    public void setLastReceivedFrame(Mat frame){
            this.lastReceivedFrame=frame;
    }
    public interface BackgroundMaskChangedListener {
        void onBackgroundMaskChanged(Mat backgroundMask);
    }
}
