package it.lorenzoranucci.hangman.threads;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Lorenzo on 29/07/2015.
 */

public class BackgroundUpdaterThread extends Thread {
    public interface BackgroundUpdaterListener {
        void onBackgroundUpdated(Mat backgroundFrame);
    }

    public void setListener(BackgroundUpdaterListener listener) {
        this.listener = listener;
    }

    private final int LIST_SIZE = 3;
    private BackgroundUpdaterListener listener;
    private Mat background;
    private double threshold = 70;
    private boolean stop = false;
    private boolean isThereANewFrame = false;

    LinkedList<Mat> list = new LinkedList<>();


    public void stopThread() {
        this.stop = true;
        listener=null;
    }

    @Override
    public void run() {
        this.stop = false;
        while (!stop) {
            if (background != null && isThereANewFrame) {
                if (listener != null && multipleDifference()) {
                    listener.onBackgroundUpdated(background);
                }
            }
        }
    }

    private boolean multipleDifference() {
        if (background!=null && list != null && list.size() == LIST_SIZE  ) {
            List<Mat> listDiff = new ArrayList<>();
            Mat foreGroundMask = Mat.zeros(list.get(0).rows(), list.get(0).cols(), CvType.CV_8UC1);
            for (int i = 0; i < list.size() - 1; i++) {
                Mat diffImage = new Mat();
                Core.absdiff(list.get(i), list.get(i + 1), diffImage);
                listDiff.add(diffImage);
            }
            for (int j = 0; j < list.get(0).rows(); ++j) {
                for (int i = 0; i < list.get(0).cols(); ++i) {
                    boolean isToCopy = true;
                    for (int k = 0; k < listDiff.size() - 1; k++) {
                        double pix[] = listDiff.get(k).get(j, i);
                        double dist = (pix[0] * pix[0] + pix[1] * pix[1] + pix[2] * pix[2]);
                        dist = Math.sqrt(dist);
                        if (dist > threshold) {
                            isToCopy = false;
                            break;
                        }
                    }
                    if (isToCopy) {
                        foreGroundMask.put(j, i, 255);//pixels to draw
                    }
                }
            }
            list.get(list.size() - 1).copyTo(background, foreGroundMask);
            return true;
        }
        return false;
    }



    public void setCurrentFrame(Mat frame) {
        isThereANewFrame = true;
        list.addLast(frame.clone());
        if (list.size() > LIST_SIZE) {
            list.removeFirst();
        }
    }

    public void setBackground(Mat frame) {
        this.background=frame.clone();
    }

}
