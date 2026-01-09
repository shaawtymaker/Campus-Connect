package com.harsh.shah.threads.clone.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

/**
 * Helper class to load Base64 images from Firebase Database
 * Use this instead of Glide for images stored in Database
 */
public class ImageLoader {

    /**
     * Load image from Firebase Database using image ID
     * @param imageView Target ImageView
     * @param imageId Image ID stored in Firebase Database
     */
    public static void loadImage(ImageView imageView, String imageId) {
        if (imageId == null || imageId.isEmpty()) {
            return;
        }

        FirebaseDatabase.getInstance().getReference("images")
                .child(imageId)
                .child("data")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null) {
                            try {
                                // Decode Base64 to bitmap
                                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                
                                // Set bitmap to ImageView
                                imageView.setImageBitmap(bitmap);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        error.toException().printStackTrace();
                    }
                });
    }

    /**
     * Load multiple images (for thread image lists)
     * Falls back to Glide if imageId looks like a URL or URI
     */
    public static void loadImageOrUrl(ImageView imageView, String imageIdOrUrl) {
        if (imageIdOrUrl == null || imageIdOrUrl.isEmpty()) {
            return;
        }

        // Check if it's a URL or URI (http, https, content, file)
        if (imageIdOrUrl.startsWith("http://") || 
            imageIdOrUrl.startsWith("https://") ||
            imageIdOrUrl.startsWith("content://") ||
            imageIdOrUrl.startsWith("file://")) {
            
            // It's a URL/URI - load with Glide
            Glide.with(imageView.getContext()).load(imageIdOrUrl).into(imageView);
        } else {
            // It's an image ID - load from Database
            loadImage(imageView, imageIdOrUrl);
        }
    }
}
