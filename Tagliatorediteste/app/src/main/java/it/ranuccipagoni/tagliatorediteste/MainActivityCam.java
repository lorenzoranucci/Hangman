package it.ranuccipagoni.tagliatorediteste;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Lorenzo on 01/07/2015.
 */
public class MainActivityCam extends Activity implements CvCameraViewListener2, View.OnTouchListener, SeekBar.OnSeekBarChangeListener {

    static {
        System.loadLibrary("opencv_java3");
    } //the name of the .so file, without the 'lib' prefix

    private CameraBridgeViewBase mOpenCvCameraView;


    private Boia boia;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            initBoia();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.main);
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.javaCamera);
            mOpenCvCameraView.setMaxFrameSize(320, 240);
            mOpenCvCameraView.enableFpsMeter();
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.setOnTouchListener(this);
            mOpenCvCameraView.enableView();
            SeekBar seekBar= (SeekBar) findViewById(R.id.threshold);
            seekBar.setOnSeekBarChangeListener(this);
            seekBar.setMax(200);
            if (boia!=null){
                seekBar.setProgress(boia.getThreshold());
            }
        }
        catch (IOException i){
            i.printStackTrace();
            setContentView(R.layout.error);
        }
    }


    private void initBoia() throws  IOException{
        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        FileOutputStream os = new FileOutputStream(mCascadeFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
        CascadeClassifier mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        this.boia=new Boia(mJavaDetector);
    }






    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();// disabilita la fotocamera
        }
        if(boia!=null){
            boia.finalize();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();// disabilita la fotocamera
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.enableView();// abilita la fotocamera
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {

    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return boia.decapita(inputFrame);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(boia!=null){
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            boia.setPointWhereToPutTheFace(event, width, height, mOpenCvCameraView.getScale());
        }
        return false;
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            Toast.makeText(this, "Phone has camera", Toast.LENGTH_LONG).show();
            return true;
        } else {
            // no camera on this device
            Toast.makeText(this, "Phone has no camera", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(boia!=null){
            boia.setThreshold(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
