package it.ranuccipagoni.tagliatorediteste;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;

import java.io.File;
import java.io.InputStream;

/**
 * Created by Lorenzo on 01/07/2015.
 */
public class MainActivityCam extends Activity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG="MainActivityCam";
    static {
        System.loadLibrary("opencv_java3");
    } //the name of the .so file, without the 'lib' prefix

    private CameraBridgeViewBase mOpenCvCameraView;
    private Boia boia;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(TAG, "Creating and setting view");
        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
        mOpenCvCameraView.enableView();
        boia=new Boia();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();// disabilita la fotocamera
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();// disabilita la fotocamera
    }



















    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    //Questo metodo viene chiamato ogni volta che la fotocamera cattura un fotogramma.
    //Riceve come parametro il Frame catturato
    //Ritorna il frame elaborato
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return boia.decapita(inputFrame);
    }


























    //Quando tocco la vista v, viene chiamato sto metodo
    //ritorna true se il metodo ha "consumato" event, false altrimenti
    //@Override
    public boolean onTouch(View v, MotionEvent event) {
        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        boia.spostaLaTesta();
        return true;
    }
}
