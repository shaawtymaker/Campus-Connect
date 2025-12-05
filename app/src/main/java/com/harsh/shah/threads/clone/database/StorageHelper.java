package com.harsh.shah.threads.clone.database;

import android.content.Context;
import android.util.Log;

import com.harsh.shah.threads.clone.Constants;
import com.harsh.shah.threads.clone.interfaces.profile.onProfileUpdate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.models.InputFile;
import io.appwrite.services.Storage;
import io.github.techgnious.IVCompressor;
import io.github.techgnious.dto.ImageFormats;
import io.github.techgnious.dto.ResizeResolution;
import io.github.techgnious.exception.ImageException;

public class StorageHelper {

    private static final String TAG = "StorageHelper";

    private static StorageHelper instance;
    private final Storage storage;

    private StorageHelper(Context context) {
        Client client = new Client(context,"https://cloud.appwrite.io/v1");
        client.setProject(Constants.APPWRITE_PROJECT_ID);
        storage = new Storage(client);
    }

    public static StorageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new StorageHelper(context);
        }
        return instance;
    }

    public void uploadFile(File file, String id) {
        storage.createFile(
                Constants.APPWRITE_STORAGE_BUCKET_ID,
                id,
                InputFile.Companion.fromPath(file.getPath()),
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        //error.printStackTrace();
                        Log.e(TAG, "StorageHelper: ", error);
                        return;
                    }

                    Log.d(TAG, result.toString());
                }));
    }

    public void deleteFile(String id) {
        storage.deleteFile(Constants.APPWRITE_STORAGE_BUCKET_ID, id, new CoroutineCallback<>((result, error)->{
            if (error != null) {
                //error.printStackTrace();
                Log.e(TAG, "StorageHelper: ", error);
                return;
            }

            Log.d(TAG, result.toString());
        }));
    }

    public void downloadFile(String id){
        storage.getFileDownload(Constants.APPWRITE_STORAGE_BUCKET_ID, id, new CoroutineCallback<>((result, error)->{
            if (error != null) {
                //error.printStackTrace();
                Log.e(TAG, "StorageHelper: ", error);
                return;
            }

            Log.d(TAG, result.toString());
        }));
    }

}
