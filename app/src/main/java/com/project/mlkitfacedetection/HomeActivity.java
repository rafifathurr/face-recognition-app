package com.project.mlkitfacedetection;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;

public class HomeActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Toast toast = null;
    private TextView stepTextView;
    boolean isUpperAxisX, isUnderAxisX, isRightAxisY, isLeftAxisY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        previewView = findViewById(R.id.previewView);
        stepTextView = findViewById(R.id.stepText);

        stepTextView.setText("Hadapkan Muka Tepat Pada Depan Kamera");
        stepTextView.setTextColor(Color.parseColor("#000000"));
        stepTextView.setVisibility(View.VISIBLE);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        isUpperAxisX = false;
        isUnderAxisX = false;
        isRightAxisY = false;
        isLeftAxisY = false;

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (toast != null) {
                    toast.cancel();
                    toast = null;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        // Image analysis detection
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                processImageProxy(imageProxy);
            }
        });

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    private void processImageProxy(ImageProxy imageProxy) {
        @SuppressWarnings("ConstantConditions")
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .enableTracking()
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.toArray().length > 0) {
                        for (Face face : faces) {
                            if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null && face.getSmilingProbability() != null) {
                                if (detectAxisX(face) && detectAxisY(face)) {
                                    stepTextView.setText("Wajah Terdeteksi");
                                    stepTextView.setTextColor(Color.parseColor("#22bb33"));
                                }
                            }
                        }
                    } else {
                        stepTextView.setText("Hadapkan Muka Tepat Pada Depan Kamera");
                        stepTextView.setTextColor(Color.parseColor("#000000"));
                        isUpperAxisX = false;
                        isUnderAxisX = false;
                        isRightAxisY = false;
                        isLeftAxisY = false;
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HomeActivity.this, "Gagal Deteksi Wajah", Toast.LENGTH_SHORT).show();
                    imageProxy.close();
                });
    }

    private boolean detectAxisX(Face face) {

        if (!isUpperAxisX || !isUnderAxisX) {
            if (!isUpperAxisX) {
                stepTextView.setText("Wajah Menghadap Keatas");
                if (face.getHeadEulerAngleX() > 10.0) {
                    isUpperAxisX = true;
                }
            } else {
                if (!isUnderAxisX) {
                    stepTextView.setText("Wajah Menghadap Kebawah");
                    if (face.getHeadEulerAngleX() < -10.0) {
                        isUnderAxisX = true;
                    }
                }
            }
        }

        return isUpperAxisX && isUnderAxisX;
    }

    private boolean detectAxisY(Face face) {

        if (!isRightAxisY || !isLeftAxisY) {
            if (!isRightAxisY) {
                stepTextView.setText("Wajah Menghadap Kekanan");
                if (face.getHeadEulerAngleY() < -10.0) {
                    isRightAxisY = true;
                }
            } else {
                if (!isLeftAxisY) {
                    stepTextView.setText("Wajah Menghadap Kekiri");
                    if (face.getHeadEulerAngleY() > 10.0) {
                        isLeftAxisY = true;
                    }
                }
            }
        }

        return isRightAxisY && isLeftAxisY;
    }
}
