package com.example.dailyselfie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.example.dailyselfie.databinding.ActivityCameraBinding;
import com.example.dailyselfie.service.RemindWorker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private Executor executor = Executors.newSingleThreadExecutor();
    private PreviewView mPreviewView;
    private FloatingActionButton mCapture,mBack,mToggle;

    private ActivityCameraBinding binding;

    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        mPreviewView = binding.cameraPreview;
        mCapture = binding.capture;
        mBack = binding.back;
        mToggle = binding.toggleLens;

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

//        mToggle.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                toggleFrontBackCamera();
//            }
//        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private void toggleFrontBackCamera() {
        lensFacing = CameraSelector.LENS_FACING_FRONT == lensFacing ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void confirmMode(boolean state) {
        if (state) {
            mCapture.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_check));
            mToggle.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_x));
        }
        else {
            mCapture.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_camera));
            mToggle.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_refresh));
        }

    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
//        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);
//
//        // Query if extension is available (optional).
//        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
//            // Enable the extension if available.
//            hdrImageCaptureExtender.enableExtension(cameraSelector);
//        }

        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);


        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraProvider.unbindAll();
                toggleFrontBackCamera();
            }
        });

        mCapture.setOnClickListener(new View.OnClickListener() {
            public String getBatchDirectoryName() {

                String app_folder_path = "";
                app_folder_path = Environment.getExternalStorageDirectory().toString() + "/DailySelfie/images";
                File dir = new File(app_folder_path);
                if (!dir.exists() && !dir.mkdirs()) {
                    Toast.makeText(CameraActivity.this, "Lỗi", Toast.LENGTH_SHORT).show();
                }

                return app_folder_path;
            }
            @Override
            public void onClick(View v) {

                SimpleDateFormat mDateFormat = new SimpleDateFormat("HH-mm-ss-dd-MM-yyyy",Locale.getDefault());
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), mDateFormat.format(new Date())+ ".jpg");


                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
                cameraProvider.unbind(preview);
                imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                confirmMode(true);
                                @SuppressLint("UnsafeOptInUsageError") Image img = image.getImage();
                                mCapture.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                                        byte[] bytes = new byte[buffer.remaining()];
                                        buffer.get(bytes);
                                        FileOutputStream outputStream = null;
                                        try {
                                            outputStream = new FileOutputStream(file);
                                            outputStream.write(bytes);
                                            remindAfterHour(1);
                                            finish();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } finally {
                                            img.close();
                                            if (outputStream != null) {
                                                try {

                                                    outputStream.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                });
                                mToggle.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        cameraProvider.unbindAll();
                                        confirmMode(false);
                                        startCamera();
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        super.onError(exception);
                    }
                });

//                mCapture.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback () {
//                            @Override
//                            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        confirmMode(false);
//                                        startCamera();
//                                        Toast.makeText(Camera_Activity.this, "Image Saved successfully", Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//
//                            }
//
//                            @Override
//                            public void onError(@NonNull ImageCaptureException error) {
//                                error.printStackTrace();
//                            }
//                        });
//                    }
//                });



            }
        });
    }

    private void remindAfterHour(int hour) {
        WorkManager.getInstance(this).cancelAllWork();
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(RemindWorker.class,hour, TimeUnit.HOURS)
                .setInitialDelay(hour, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this).enqueue(periodicWorkRequest);
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Bạn cần phải cấp quyền máy ảnh và lưu trữ.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}