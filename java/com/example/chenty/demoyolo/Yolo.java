package com.example.chenty.demoyolo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;

import android.view.MenuItem;

import android.os.Handler;
import android.os.Message;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;

import android.net.Uri;
import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.Toast;

public class Yolo extends AppCompatActivity {
    private static final int COPY_FALSE = -1;
    private static final int DETECT_FINISH = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 2;
    private static final String TAG = "Yolo";
    private static final int RESULT_LOAD_IMAGE = 3;
    private static final int RESULT_LOAD_CAMERA = 4;
    private Uri imageUri;
    private static String[] PERMISSIONS_SOME = {
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.MOUNT_UNMOUNT_FILESYSTEMS",
            "android.permission.CAMERA"
    };

    ImageView view_srcimg;
    ImageView view_dstimg;
    TextView view_status;
    Bitmap dstimg;
    Bitmap srcimg;
    //Button btn1;

    String srcimgpath;


    static {
        System.loadLibrary("darknetlib");
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DETECT_FINISH) {
                dstimg = BitmapFactory.decodeFile("/sdcard/yolo/out.png");
                view_dstimg.setImageBitmap(dstimg);
                view_status.setText("run time = " + (double)msg.obj + "s");
            }
            else
            if (msg.what == COPY_FALSE) {

            }
        }
    };

    public Yolo() {
        srcimgpath = "/sdcard/yolo/data/car.jpg";
    }

    public void copyFilesFassets(Context context, String oldPath, String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                File file = new File(newPath);
                file.mkdirs();
                for (String fileName : fileNames) {
                    copyFilesFassets(context,oldPath + "/" + fileName,newPath+"/"+fileName);
                }
            } else {
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount=0;
                while((byteCount=is.read(buffer))!=-1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            mHandler.sendEmptyMessage(COPY_FALSE);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yolo);
        verifyStoragePermissions(this);
        view_srcimg = (ImageView) findViewById(R.id.srcimg);
        view_dstimg = (ImageView) findViewById(R.id.dstimg);
        view_status = (TextView) findViewById(R.id.status);

        dstimg = BitmapFactory.decodeFile("/sdcard/out.png");
        view_dstimg.setImageBitmap(dstimg);
        view_dstimg.setScaleType(ImageView.ScaleType.FIT_XY);

        view_srcimg.setScaleType(ImageView.ScaleType.FIT_XY);

    }

    public static void verifyStoragePermissions(Activity activity){
        for(String PERMISSION : PERMISSIONS_SOME) {
            try {
                int permission = ActivityCompat.checkSelfPermission(activity, PERMISSION);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, PERMISSIONS_SOME, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null,
                null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case RESULT_LOAD_IMAGE:
                if (resultCode == RESULT_OK && null != data) {
                    Uri uri = data.getData();

                    srcimgpath = getImagePath(uri, null);
                    view_status.setText("selectfile = " + srcimgpath);
                    srcimg = BitmapFactory.decodeFile(srcimgpath);
                    view_srcimg.setImageBitmap(srcimg);
                }
                break;
            case RESULT_LOAD_CAMERA:
                if (resultCode == RESULT_OK){
                    //File picture = new File(Environment.getExternalStorageDirectory() + "/temp.jpg");
                    try{
                        File outputImage = new File(Environment.getExternalStorageDirectory(), "output_image.jpg");
                        srcimgpath = outputImage.getAbsolutePath();
                        srcimg = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        view_status.setText("getting from camera");
                        view_srcimg.setImageBitmap(srcimg);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    public void yoloDetect(){

        new Thread(new Runnable() {
            public void run() {
                double runtime = testyolo(srcimgpath);
                Log.i(TAG, "yolo run time " + runtime);
                Message msg = new Message();
                msg.what = DETECT_FINISH;
                msg.obj = runtime;
                mHandler.sendMessage(msg);
            }
        }).start();

    }


    public void exactresClick(View v){
        view_status.setText("load model, please wait");
        copyFilesFassets(this, "cfg", "/sdcard/yolo/cfg");
        copyFilesFassets(this, "data", "/sdcard/yolo/data");
        copyFilesFassets(this, "weights", "/sdcard/yolo/weights");
        view_status.setText("load model finishing");

    }

    public void analyseClick(View v){

        view_dstimg.setImageResource(R.drawable.yologo_1);
        view_status.setText("Analysing ...");
        yoloDetect();
    }

    public void captureClick(View v){
        yoloDetect();
    }
    
    public void selectimgClick(View v){
        Intent i = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    public void cameraClick(View v){
        File outputImage = new File(Environment.getExternalStorageDirectory(), "output_image.jpg");
        try{
            if (outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 24){
            imageUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.chenty.demoyolo.fileprovider", outputImage);
        }else {
            imageUri = Uri.fromFile(outputImage);
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, RESULT_LOAD_CAMERA);
    }

    protected void getImageFromCamera(){
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "tem.jpg")));
            startActivityForResult(intent, RESULT_LOAD_CAMERA);
        }else {
            Toast.makeText(this, "", Toast.LENGTH_LONG).show();
        }
    }

    public void rebutClick(View v){
//        AlertDialog.Builder ab = new AlertDialog.Builder(getApplicationContext());
//        ab.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//            @Override


        Toast.makeText(getApplicationContext(), "No Server", Toast.LENGTH_SHORT).show();
    }


    public native void inityolo(String cfgfile, String weightfile);
    public native double testyolo(String imgfile);
    public native boolean detectimg(Bitmap dst, Bitmap src);
}
