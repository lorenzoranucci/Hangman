package it.ranuccipagoni.tagliatorediteste;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Lorenzo on 01/07/2015.
 */
public class MainActivityCam extends Activity implements  CvCameraViewListener2, View.OnTouchListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    static {
        System.loadLibrary("opencv_java3");
    } //the name of the .so file, without the 'lib' prefix

    private CameraBridgeViewBase mOpenCvCameraView;


    private Boia boia;

    private boolean isToSaveBitmap=false;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            initBoia();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.main);
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.javaCamera);
            mOpenCvCameraView.setMaxFrameSize(640, 480);
            mOpenCvCameraView.enableFpsMeter();
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.setOnTouchListener(this);
            mOpenCvCameraView.enableView();
            SeekBar seekBar= (SeekBar) findViewById(R.id.threshold);
            seekBar.setOnSeekBarChangeListener(this);
            seekBar.setMax(140);
            Button buttonBg= (Button) findViewById(R.id.backgroundButton);
            buttonBg.setOnClickListener(this);
            ImageButton buttonCapture= (ImageButton) findViewById(R.id.captureButton);
            buttonCapture.setOnClickListener(this);
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
            mOpenCvCameraView.disableView();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.enableView();
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
        Mat mat=boia.decapitate(inputFrame);
        if(isToSaveBitmap && isExternalStorageWritable()){
            isToSaveBitmap=false;
            Bitmap bmp;
            try {
                bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bmp);
                File dir = getAlbumStorageDir("Boia");
                String path =dir.getPath()+File.separator+ "boia" +System.currentTimeMillis() + ".JPG";
                File capture= new File(path);
                OutputStream out = null;
                try {
                    capture.createNewFile();
                    out = new FileOutputStream(capture);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                }  catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Exception exc) {
                    }
                }
            }
            catch (CvException e) {
                Log.d("Exception", e.getMessage());
            }
        }
        return mat;
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


    @Override
    public void onClick(View v) {
        if (boia != null) {
            if (v.getId() == R.id.backgroundButton) {
                boia.setBackground();
            } else if (v.getId() == R.id.captureButton) {
                isToSaveBitmap = true;
            }
        }
    }
     /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), albumName);
        if (!file.mkdirs()) {
            Log.e("CAPTURE", "Directory not created");
        }
        return file;
    }
}




