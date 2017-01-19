package it.lorenzoranucci.hangman.activities;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import it.lorenzoranucci.hangman.workers.Hangman;
import it.lorenzoranucci.hangman.R;

/**
 * Created by Lorenzo on 01/07/2015.
 */
public class MainActivityCam extends Activity implements  CvCameraViewListener2, View.OnTouchListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener,AdapterView.OnItemSelectedListener {

    static {
        System.loadLibrary("opencv_java3");
    } //the name of the .so file, without the 'lib' prefix

    private JavaCameraView mOpenCvCameraView;


    private Hangman hangman;

    private boolean isToSaveBitmap=false;
    private int mCameraId = 0;

    private boolean intentImageCapture;
    private boolean isToManuallyUpdateBackground=false;


    /*Callback for activity creation*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            /*init hangman worker object*/
            initHangman();

            setContentView(R.layout.main);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            mOpenCvCameraView = (JavaCameraView) findViewById(R.id.javaCamera);
            //mOpenCvCameraView.setMaxFrameSize(640, 480);
            mOpenCvCameraView.setCameraIndex(mCameraId);
            mOpenCvCameraView.enableFpsMeter();
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.setOnTouchListener(this);
            mOpenCvCameraView.enableView();
            //mOpenCvCameraView.getmCamera().setParameters(params);

            /*create button for background init*/
            ImageButton buttonBg= (ImageButton) findViewById(R.id.backgroundButton);
            buttonBg.setOnClickListener(this);

            /*create button for picture capture*/
            ImageButton buttonCapture= (ImageButton) findViewById(R.id.captureButton);
            buttonCapture.setOnClickListener(this);

            /*create button for camera switch*/
            ImageButton changeCamera= (ImageButton) findViewById(R.id.changeCamera);
            changeCamera.setOnClickListener(this);

            Spinner spinner = (Spinner) findViewById(R.id.selectBGSubtractor);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.background_subtractors_available, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);

            SeekBar seekBarT= (SeekBar) findViewById(R.id.threshold);
            seekBarT.setOnSeekBarChangeListener(this);
            seekBarT.setMax(100);
            seekBarT.setProgress(50);

            SeekBar seekBarQ= (SeekBar) findViewById(R.id.quality);
            seekBarQ.setOnSeekBarChangeListener(this);
            seekBarQ.setMax(100);
            seekBarQ.setProgress(50);


            /*check if app is launched with intent image capture*/
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
        /*open OpenCV cascade file for face recognition*/
        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);

        /*copy it in a temporary file*/
        File mCascadeFile =File.createTempFile("lbpcascade_frontalface","xml");
        FileOutputStream os = new FileOutputStream(mCascadeFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();

        /*Create the FaceMaskGeneratorCIE1994 object and pass it the cascade file path*/
        CascadeClassifier mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        this.hangman =new Hangman(mJavaDetector,Hangman.PIXEL_COLOR_BASED);
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



    /*called every time the camera view send a frame to the activity (every frame it captures)*/
    @Override
    public synchronized Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if(isToManuallyUpdateBackground){
            isToManuallyUpdateBackground=false;
            hangman.initBackground(inputFrame);
        }
        /*apply image effects to new frame*/
        Mat mat= hangman.decapitate(inputFrame);
        /*check if users clicked the capture button and if external storage is writable for save it*/
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

                    /*Inform the media store that a new image was saved*/
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA, path);
                    getBaseContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    /*if app was called with intent image capture */
                    if(intentImageCapture){
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
                finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (CvException e) {
                Log.e("Exception", e.getMessage());
                e.printStackTrace();
            }
        }
        return mat;
    }



    /*call back for camera view touch event*/
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(hangman !=null){
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            hangman.setFaceDestination(event, width, height, mOpenCvCameraView.getmScale());
        }
        return false;
    }

    /*call back for seek bars interactions*/
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(hangman !=null){
            String paramName;
            if(seekBar.getId()==R.id.quality){
                paramName="quality";
            }
            else{
                paramName="threshold";
            }
            Map<String,String> params=new HashMap<>();
            params.put(paramName,String.valueOf(progress));
            hangman.getFaceMaskGenerator().setParameters(params);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if(seekBar.getId()==R.id.quality){
            seekBar.setProgress(10);
        }
        else  if(seekBar.getId()==R.id.threshold){
            seekBar.setProgress(16);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    /*call back for buttons touch interactions*/
    @Override
    public void onClick(View v) {
        if (hangman != null) {
            if (v.getId() == R.id.backgroundButton) {
                isToManuallyUpdateBackground=true;
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
        if (!file.exists() && !file.mkdirs()) {
            Log.e("CAPTURE", "Directory not created");
        }
        return file;
    }
    private void swapCamera() {
        mCameraId = mCameraId^1; //bitwise not operation to flip 1 to 0 and vice versa
        mOpenCvCameraView.disableView();
        if (hangman !=null){
            hangman.stop();
        }
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch(position){
            case 0: hangman.setFaceMaskGenerator(Hangman.PIXEL_COLOR_BASED);
                break;
            case 1: hangman.setFaceMaskGenerator(Hangman.MOG2);
                break;
            case 2: hangman.setFaceMaskGenerator(Hangman.KNN);
                break;
            default: hangman.setFaceMaskGenerator(Hangman.PIXEL_COLOR_BASED);
        }

        SeekBar seekBarT= (SeekBar) findViewById(R.id.threshold);
        SeekBar seekBarQ= (SeekBar) findViewById(R.id.quality);
        View seekBarTL=  findViewById(R.id.threshold_label);
        View seekBarQL=  findViewById(R.id.quality_label);
        seekBarT.setVisibility(View.GONE);
        seekBarQ.setVisibility(View.GONE);
        seekBarTL.setVisibility(View.GONE);
        seekBarQL.setVisibility(View.GONE);
        Map<String,String> params=hangman.getFaceMaskGenerator().getParameters();
        if(params!=null){
            if(params.containsKey("threshold")){
                seekBarT.setVisibility(View.VISIBLE);
                seekBarTL.setVisibility(View.VISIBLE);
            }
            if(params.containsKey("quality")){
                seekBarQ.setVisibility(View.VISIBLE);
                seekBarQL.setVisibility(View.VISIBLE);
            }
        }
        seekBarQ.setProgress(50);
        seekBarT.setProgress(50);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


}



