package com.example.tamz2faces;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1; // Request code for gallery
    private ImageView imageView;
    private Button galleryButton, detectButton, replaceButton, christmasButton, christmasPlusButton;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.defaultImageView);
        galleryButton = findViewById(R.id.galleryButton);
        detectButton = findViewById(R.id.detectButton);
        replaceButton = findViewById(R.id.replaceButton);
        christmasButton = findViewById(R.id.christmasButton);
        christmasPlusButton = findViewById(R.id.christmasPlusButton);


        replaceButton.setEnabled(false);
        christmasButton.setEnabled(false);
        christmasPlusButton.setEnabled(false);


        galleryButton.setOnClickListener(v -> openGallery());


        detectButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                detectFaces(selectedImageUri);
            } else {
                Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            }
        });


        replaceButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                replaceFaces(selectedImageUri);
            } else {
                Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            }
        });

        // Button to add Santa hats
        christmasButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                addChristmasHats(selectedImageUri);
                showSpinningSnowflakes();
            } else {
                Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            }
        });


        christmasPlusButton.setOnClickListener(v -> showRandomSnowflakes());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                imageView.setImageURI(selectedImageUri);
                replaceButton.setEnabled(false);
                christmasButton.setEnabled(false);
                christmasPlusButton.setEnabled(false);
            }
        }
    }

    private void detectFaces(Uri imageUri) {
        try {
            Bitmap originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, 1024, 960); // Resize for consistent input size
            InputImage image = InputImage.fromBitmap(resizedBitmap, 0);

            // Configure ML Kit face detector options
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();

            // Process image with face detector
            FaceDetection.getClient(options).process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces != null && !faces.isEmpty()) {
                            Log.d("FaceDetection", "Detected faces count: " + faces.size());
                            replaceButton.setEnabled(true); // Enable buttons
                            christmasButton.setEnabled(true);
                            christmasPlusButton.setEnabled(true);
                            drawFaceAnnotations(faces, originalBitmap, resizedBitmap);
                        } else {
                            Toast.makeText(this, "No faces detected!", Toast.LENGTH_SHORT).show();
                            replaceButton.setEnabled(false); // Disable buttons
                            christmasButton.setEnabled(false);
                            christmasPlusButton.setEnabled(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Face detection failed!", Toast.LENGTH_SHORT).show();
                        replaceButton.setEnabled(false); // Disable buttons on failure
                        christmasButton.setEnabled(false);
                        christmasPlusButton.setEnabled(false);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawFaceAnnotations(List<Face> faces, Bitmap originalBitmap, Bitmap resizedBitmap) {
        try {
            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);

            float scaleX = (float) originalBitmap.getWidth() / resizedBitmap.getWidth();
            float scaleY = (float) originalBitmap.getHeight() / resizedBitmap.getHeight();

            for (Face face : faces) {
                Rect detectedBounds = face.getBoundingBox();

                // Scale bounding box back to original image size
                int left = (int) (detectedBounds.left * scaleX);
                int top = (int) (detectedBounds.top * scaleY);
                int right = (int) (detectedBounds.right * scaleX);
                int bottom = (int) (detectedBounds.bottom * scaleY);
                Rect scaledBounds = new Rect(left, top, right, bottom);

                // Create square bounds
                int size = Math.min(scaledBounds.width(), scaledBounds.height());
                int centerX = scaledBounds.centerX();
                int centerY = scaledBounds.centerY();
                int squareLeft = centerX - size / 2;
                int squareTop = centerY - size / 2;
                int squareRight = centerX + size / 2;
                int squareBottom = centerY + size / 2;
                Rect squareBounds = new Rect(squareLeft, squareTop, squareRight, squareBottom);

                // Draw square
                canvas.drawRect(squareBounds, paint);

                // Draw circle inside square
                float radius = size / 2f;
                canvas.drawCircle(squareBounds.exactCenterX(), squareBounds.exactCenterY(), radius, paint);

                // Draw contours
                if (face.getContour(FaceContour.LEFT_EYE) != null) {
                    drawContour(canvas, face.getContour(FaceContour.LEFT_EYE).getPoints(), paint, scaleX, scaleY);
                }
                if (face.getContour(FaceContour.RIGHT_EYE) != null) {
                    drawContour(canvas, face.getContour(FaceContour.RIGHT_EYE).getPoints(), paint, scaleX, scaleY);
                }
                if (face.getContour(FaceContour.UPPER_LIP_BOTTOM) != null) {
                    drawContour(canvas, face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints(), paint, scaleX, scaleY);
                }
            }

            imageView.setImageBitmap(mutableBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawContour(Canvas canvas, List<PointF> points, Paint paint, float scaleX, float scaleY) {
        if (points == null || points.size() < 2) {
            return;
        }
        for (int i = 0; i < points.size() - 1; i++) {
            PointF start = points.get(i);
            PointF end = points.get(i + 1);

            float startX = start.x * scaleX;
            float startY = start.y * scaleY;
            float endX = end.x * scaleX;
            float endY = end.y * scaleY;

            canvas.drawLine(startX, startY, endX, endY, paint);
        }

        PointF first = points.get(0);

        PointF last = points.get(points.size() - 1);

        canvas.drawLine(last.x * scaleX, last.y * scaleY, first.x * scaleX, first.y * scaleY, paint);
    }

    private void replaceFaces(Uri imageUri) {
        try {
            Bitmap originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, 1024, 960); // Resize for consistent input
            InputImage image = InputImage.fromBitmap(resizedBitmap, 0);

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();

            FaceDetection.getClient(options).process(image)
                    .addOnSuccessListener(faces -> drawEmojiOnFaces(faces, originalBitmap, resizedBitmap))
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Face replacement failed!", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawEmojiOnFaces(List<Face> faces, Bitmap originalBitmap, Bitmap resizedBitmap) {
        try {
            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Bitmap emojiBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.emoji_image);

            float scaleX = (float) originalBitmap.getWidth() / resizedBitmap.getWidth();
            float scaleY = (float) originalBitmap.getHeight() / resizedBitmap.getHeight();

            for (Face face : faces) {
                Rect detectedBounds = face.getBoundingBox();

                int left = (int) (detectedBounds.left * scaleX);
                int top = (int) (detectedBounds.top * scaleY);
                int right = (int) (detectedBounds.right * scaleX);
                int bottom = (int) (detectedBounds.bottom * scaleY);
                Rect scaledBounds = new Rect(left, top, right, bottom);

                Bitmap resizedEmoji = Bitmap.createScaledBitmap(emojiBitmap, scaledBounds.width(), scaledBounds.height(), false);
                canvas.drawBitmap(resizedEmoji, scaledBounds.left, scaledBounds.top, null);
            }

            imageView.setImageBitmap(mutableBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addChristmasHats(Uri imageUri) {
        try {
            Bitmap originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, 720, 600); // Resize for consistent input
            InputImage image = InputImage.fromBitmap(resizedBitmap, 0);

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();

            FaceDetection.getClient(options).process(image)
                    .addOnSuccessListener(faces -> drawChristmasHats(faces, originalBitmap, resizedBitmap))
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to add Christmas hats!", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawChristmasHats(List<Face> faces, Bitmap originalBitmap, Bitmap resizedBitmap) {
        try {
            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Bitmap hatBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.santa_hat);

            float scaleX = (float) originalBitmap.getWidth() / resizedBitmap.getWidth();
            float scaleY = (float) originalBitmap.getHeight() / resizedBitmap.getHeight();

            for (Face face : faces) {
                Rect detectedBounds = face.getBoundingBox();

                int left = (int) (detectedBounds.left * scaleX);
                int top = (int) (detectedBounds.top * scaleY);
                int right = (int) (detectedBounds.right * scaleX);
                int bottom = (int) (detectedBounds.bottom * scaleY);

                Rect scaledBounds = new Rect(left, top, right, bottom);

                int size = Math.min(scaledBounds.width(), scaledBounds.height());
                int centerX = scaledBounds.centerX();
                int squareLeft = centerX - size / 2;
                int squareTop = top - (int) (size * 0.6);
                int squareRight = centerX + size / 2;
                int squareBottom = squareTop + size;

                Rect squareBounds = new Rect(squareLeft, squareTop, squareRight, squareBottom);

                // Draw the Santa hat within square bounds
                Bitmap resizedHat = Bitmap.createScaledBitmap(hatBitmap, squareBounds.width(), squareBounds.height(), false);
                canvas.drawBitmap(resizedHat, squareBounds.left, squareBounds.top, null);
            }

            // Set the final image with hats
            imageView.setImageBitmap(mutableBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);

        return Bitmap.createScaledBitmap(bitmap, (int) (width * scale), (int) (height * scale), true);
    }

    private void showSpinningSnowflakes() {

        ImageView snowflake1 = findViewById(R.id.snowflake1);
        ImageView snowflake2 = findViewById(R.id.snowflake2);
        ImageView snowflake3 = findViewById(R.id.snowflake3);


        snowflake1.setVisibility(View.VISIBLE);
        snowflake2.setVisibility(View.VISIBLE);
        snowflake3.setVisibility(View.VISIBLE);


        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        rotate.setDuration(1000);

        rotate.setRepeatCount(Animation.INFINITE);

        snowflake1.startAnimation(rotate);
        snowflake2.startAnimation(rotate);
        snowflake3.startAnimation(rotate);

        ObjectAnimator fadeOut1 = ObjectAnimator.ofFloat(snowflake1, "alpha", 1f, 0f);
        ObjectAnimator fadeOut2 = ObjectAnimator.ofFloat(snowflake2, "alpha", 1f, 0f);
        ObjectAnimator fadeOut3 = ObjectAnimator.ofFloat(snowflake3, "alpha", 1f, 0f);

        fadeOut1.setDuration(10000);
        fadeOut2.setDuration(10000);
        fadeOut3.setDuration(10000);

        fadeOut1.start();
        fadeOut2.start();
        fadeOut3.start();

        fadeOut3.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                snowflake1.setVisibility(View.GONE);
                snowflake2.setVisibility(View.GONE);
                snowflake3.setVisibility(View.GONE);
            }
        });
    }

    private void showRandomSnowflakes() {
        FrameLayout rootLayout = findViewById(android.R.id.content);

        for (int i = 0; i < 100; i++) {

            ImageView snowflake = new ImageView(this);
            snowflake.setImageResource(R.drawable.snow_flake);
            snowflake.setLayoutParams(new FrameLayout.LayoutParams(64, 64));

            int size = (int) (Math.random() * 50) + 30;
            snowflake.setLayoutParams(new FrameLayout.LayoutParams(size, size));

            int startX = (int) (Math.random() * rootLayout.getWidth());
            int startY = (int) (Math.random() * rootLayout.getHeight() / 2);
            snowflake.setX(startX);
            snowflake.setY(startY);

            rootLayout.addView(snowflake);

            int endY = rootLayout.getHeight();
            ObjectAnimator fallAnimation = ObjectAnimator.ofFloat(snowflake, "translationY", startY, endY);
            fallAnimation.setDuration((long) (Math.random() * 3000) + 2000);

            ObjectAnimator fadeOutAnimation = ObjectAnimator.ofFloat(snowflake, "alpha", 1f, 0f);
            fadeOutAnimation.setDuration((long) (Math.random() * 2000) + 2000);
            fadeOutAnimation.setStartDelay((long) (Math.random() * 2000));

            AnimatorSet animatorSet = new AnimatorSet();

            animatorSet.playTogether(fallAnimation, fadeOutAnimation);

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    rootLayout.removeView(snowflake);
                }
            });

            animatorSet.start();
        }
    }

}
