package it.ranuccipagoni.tagliatorediteste;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity {
    private static int RESULT_LOAD_IMAGE = 1;

    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private static final String TAG = "Tagliatore di teste::MainActivity";


    static {
        System.loadLibrary("opencv_java3");
    } //the name of the .so file, without the 'lib' prefix


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnLoadPicture = (Button) findViewById(R.id.buttonLoadPicture);

        btnLoadPicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);

            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            // String picturePath contains the path of selected Image
            //ImageView imageView = (ImageView) findViewById(R.id.imgView);
            //imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            Bitmap bm = BitmapFactory.decodeFile(picturePath);
            Bitmap bm2=BitmapFactory.decodeFile(picturePath) ;
            Mat image = new Mat();
            Utils.bitmapToMat(bm, image);


            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            try {
                FileOutputStream os = new FileOutputStream(mCascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();


                mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());

                // Detect faces in the image.
                // MatOfRect is a special container class for Rect.
                MatOfRect faceDetections = new MatOfRect();
                mJavaDetector.detectMultiScale(image, faceDetections);


                System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
                Rect [] recta = faceDetections.toArray();
                Rect r = recta[0];

                Point point= new Point(r.x + r.width*0.5, r.y + r.height*0.5 );
                Size size= new Size(r.width,r.height);
                Scalar color= new Scalar(0,255,0);
               // Imgproc.ellipse(image, point, new Size(r.width * 0.5, r.height * 0.5), 0, 0, 360, new Scalar(255, 0, 255), 4, 8, 0);
                //Imgproc.rectangle(image, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(0, 255, 0));
                Mat bgd=new Mat();
                Mat fgd= new Mat();
                Mat mask = new Mat(image.size(),CvType.CV_8U);

                Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
                Imgproc.grabCut(image, mask, r, bgd, fgd, 1,Imgproc.GC_INIT_WITH_MASK);



                Utils.matToBitmap(image, bm);
                Utils.matToBitmap(mask,bm2);

                ImageView imgView = (ImageView) findViewById(R.id.imageView);
                imgView.setImageBitmap(bm);
                ImageView imgView2 = (ImageView) findViewById(R.id.imageView2);
                imgView2.setImageBitmap(bm2);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
