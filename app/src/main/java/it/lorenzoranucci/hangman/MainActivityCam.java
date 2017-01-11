package it.lorenzoranucci.hangman;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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


    private Hangman hangman;

    private boolean isToSaveBitmap=false;
    private int mCameraId = 0;

    private boolean intentImageCapture;


    //of activity

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            initHangman();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.main);
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.javaCamera);
            mOpenCvCameraView.setMaxFrameSize(640, 480);
            mOpenCvCameraView.setCameraIndex(mCameraId);
            mOpenCvCameraView.enableFpsMeter();
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.setOnTouchListener(this);
            mOpenCvCameraView.enableView();
            SeekBar seekBarT= (SeekBar) findViewById(R.id.threshold);
            seekBarT.setOnSeekBarChangeListener(this);
            seekBarT.setMax(45);
            SeekBar seekBarQ= (SeekBar) findViewById(R.id.quality);
            seekBarQ.setOnSeekBarChangeListener(this);
            seekBarQ.setMax(90);
            ImageButton buttonBg= (ImageButton) findViewById(R.id.backgroundButton);
            buttonBg.setOnClickListener(this);
            ImageButton buttonCapture= (ImageButton) findViewById(R.id.captureButton);
            buttonCapture.setOnClickListener(this);
            ImageButton changeCamera= (ImageButton) findViewById(R.id.changeCamera);
            changeCamera.setOnClickListener(this);
            if (hangman !=null){
                seekBarT.setProgress(hangman.getThreshold());
            }

            // Get the intent that started this activity, to put this app in photo chooser
            Intent intent = getIntent();
            intentImageCapture=false;
            if(intent.getAction().equals("android.media.action.IMAGE_CAPTURE"))
                intentImageCapture=true;


        }
        catch (IOException i){
            i.printStackTrace();
            setContentView(R.layout.error);
        }
    }


    private void initHangman() throws  IOException{
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
        this.hangman =new Hangman(mJavaDetector);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if(hangman !=null){
            hangman.stopThread();
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
        if(hangman !=null){
            hangman.startThread();
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
        Mat mat= hangman.decapitate(inputFrame);
        if(isToSaveBitmap && isExternalStorageWritable()){
            isToSaveBitmap=false;
            Bitmap bmp;
            try {
                bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bmp);
                File dir = getAlbumStorageDir("hangman");
                String path =dir.getPath()+File.separator+ "hangman" +System.currentTimeMillis() + ".JPG";
                File capture= new File(path);
                OutputStream out = null;
                try {
                    capture.createNewFile();
                    out = new FileOutputStream(capture);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                    if(intentImageCapture){
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (Exception exc) {
                        }
                        Intent result = new Intent("com.example.RESULT_ACTION");
                        result.setData(Uri.fromFile(capture));
                        result.putExtra(Intent.EXTRA_STREAM,capture);
                        result.setType("image/jpg");
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }
                }  catch (IOException e) {
                    e.printStackTrace();
                }
            }
            catch (CvException e) {
                Log.d("Exception", e.getMessage());
            }
        }
        return mat;
    }



    //of view
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(hangman !=null){
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            hangman.setPointWhereToPutTheFace(event, width, height, mOpenCvCameraView.getmScale());
        }
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(hangman !=null){
            if(seekBar.getId()==R.id.quality){
                hangman.setQuality(progress);
            }
            else  if(seekBar.getId()==R.id.threshold){
                hangman.setThreshold(progress);
            }

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
        if (hangman != null) {
            if (v.getId() == R.id.backgroundButton) {
                hangman.setBackground();
            } else if (v.getId() == R.id.captureButton) {
                isToSaveBitmap = true;
            } else if (v.getId() == R.id.changeCamera) {
                swapCamera();
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
    private void swapCamera() {
        mCameraId = mCameraId^1; //bitwise not operation to flip 1 to 0 and vice versa
        mOpenCvCameraView.disableView();
        if (hangman !=null){
            hangman.setNullBackground();
        }
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }
}



