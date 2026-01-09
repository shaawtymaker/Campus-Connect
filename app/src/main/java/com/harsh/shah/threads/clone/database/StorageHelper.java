package com.harsh.shah.threads.clone.database;

import android.content.Context;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class StorageHelper {

    private static final String TAG = "StorageHelper";
    private static final String STORAGE_PATH = "thread_images";

    private static StorageHelper instance;
    private final FirebaseStorage storage;

    private StorageHelper(Context context) {
        storage = FirebaseStorage.getInstance();
    }

    public static StorageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new StorageHelper(context);
        }
        return instance;
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    public void uploadFile(byte[] bytes, String fileName, String id, UploadCallback callback) {
        try {
            // Create reference to image location
            StorageReference imageRef = storage.getReference()
                    .child(STORAGE_PATH)
                    .child(id + ".jpg");

            Log.d(TAG, "Attempting upload: " + fileName + " (ID: " + id + ")");
            Log.d(TAG, "Storage path: " + STORAGE_PATH + "/" + id + ".jpg");

            // Upload the file
            UploadTask uploadTask = imageRef.putBytes(bytes);
            
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL after successful upload
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "Upload success: " + downloadUrl);
                    if (callback != null) callback.onSuccess(downloadUrl);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL: ", e);
                    if (callback != null) callback.onFailure(e);
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Upload failed: ", e);
                Log.e(TAG, "Error details: " + e.getMessage());
                if (callback != null) callback.onFailure(e);
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception during upload setup: ", e);
            if (callback != null) callback.onFailure(e);
        }
    }

    public void deleteFile(String id) {
        StorageReference imageRef = storage.getReference()
                .child(STORAGE_PATH)
                .child(id + ".jpg");
        
        imageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Delete success: " + id);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Delete failed: ", e);
                });
    }
}
