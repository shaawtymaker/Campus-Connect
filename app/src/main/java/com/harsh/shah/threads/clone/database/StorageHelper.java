package com.harsh.shah.threads.clone.database;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageHelper {

    private static final String TAG = "StorageHelper";
    private static final String STORAGE_NODE = "images";

    private static StorageHelper instance;
    private final DatabaseReference imagesRef;

    private StorageHelper(Context context) {
        imagesRef = FirebaseDatabase.getInstance().getReference(STORAGE_NODE);
    }

    public static StorageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new StorageHelper(context);
        }
        return instance;
    }

    public interface UploadCallback {
        void onSuccess(String imageId);
        void onFailure(Exception e);
    }

    public void uploadFile(byte[] bytes, String fileName, String id, UploadCallback callback) {
        try {
            Log.d(TAG, "Converting image to Base64: " + fileName + " (ID: " + id + ")");
            
            // Convert bytes to Base64 string
            String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);
            
            // Create image data
            Map<String, Object> imageData = new HashMap<>();
            imageData.put("data", base64Image);
            imageData.put("fileName", fileName);
            imageData.put("uploadTime", System.currentTimeMillis());
            
            // Store in Firebase Database under "images/{id}"
            imagesRef.child(id).setValue(imageData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Image stored successfully: " + id);
                        if (callback != null) callback.onSuccess(id);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to store image: ", e);
                        if (callback != null) callback.onFailure(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Exception during image encoding: ", e);
            if (callback != null) callback.onFailure(e);
        }
    }

    public void deleteFile(String id) {
        imagesRef.child(id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Delete success: " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Delete failed: ", e);
                });
    }
    
    /**
     * Get Base64 data for an image ID
     * Use this with a custom Glide loader or decode manually
     */
    public interface ImageLoadCallback {
        void onImageLoaded(String base64Data);
        void onFailure(Exception e);
    }
    
    public void getImageData(String imageId, ImageLoadCallback callback) {
        imagesRef.child(imageId).child("data").get()
                .addOnSuccessListener(snapshot -> {
                    String base64 = snapshot.getValue(String.class);
                    if (base64 != null && callback != null) {
                        callback.onImageLoaded(base64);
                    } else if (callback != null) {
                        callback.onFailure(new Exception("Image not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }
}
