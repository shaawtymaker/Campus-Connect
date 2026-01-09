package com.harsh.shah.threads.clone;

import static android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.activities.MainActivity;
import com.harsh.shah.threads.clone.activities.ProfileActivity;
import com.harsh.shah.threads.clone.activities.SplashActivity;
import com.harsh.shah.threads.clone.model.UserModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class BaseActivity extends AppCompatActivity {

    public static final String TAG = "BaseActivity";
    public static DatabaseReference mUsersDatabaseReference;
    public static UserModel mUser;
    public static DatabaseReference mThreadsDatabaseReference;
    public static DatabaseReference mFollowRequestsDatabaseReference;
    public static DatabaseReference mNotificationsDatabaseReference;
    public FirebaseAuth mAuth;
    public FirebaseDatabase mDatabase;
    public DatabaseReference gUsernamesDatabaseReference;
    public GoogleSignInOptions googleSignInOptions;
    public GoogleSignInClient googleSignInClient;
    public AlertDialog progressDialog;
    public OnCompleteListener<AuthResult> authResultTask = task -> {
        if (!task.isSuccessful()) {
            showToast("Error: " + task.getException());
            return;
        }

        loginTask(task);
    };
    private final ValueEventListener usersValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                UserModel user = dataSnapshot.getValue(UserModel.class);
                if (user != null && user.getUid().equals(mAuth.getCurrentUser().getUid())) {
                    mUser = user;
                    break;
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    private ValueEventListener currentUserListener;

    public void fetchCurrentUser(OnUserLoadedListener listener) {
        fetchCurrentUser(false, listener);
    }

    public void fetchCurrentUser(boolean forceRefresh, OnUserLoadedListener listener) {
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "fetchCurrentUser: No current auth user");
            if (listener != null) listener.onUserLoaded(null);
            return;
        }
        String currentUid = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "fetchCurrentUser: Fetching for UID " + currentUid + ", forceRefresh=" + forceRefresh);
        Log.d(TAG, "fetchCurrentUser: Database path: " + mUsersDatabaseReference.toString());
        
        // If we already have the user and not forcing refresh, and we have a listener, return cache
        if (!forceRefresh && mUser != null && mUser.getUid().equals(currentUid) && currentUserListener != null) {
            Log.d(TAG, "fetchCurrentUser: User already loaded in memory (using cache)");
            if (listener != null) listener.onUserLoaded(mUser);
            return;
        }

        // Remove old listener if exists
        if (currentUserListener != null) {
            mUsersDatabaseReference.removeEventListener(currentUserListener);
        }

        // Query users by UID and attach a persistent listener
        Log.d(TAG, "fetchCurrentUser: Starting persistent Firebase listener for UID " + currentUid);
        currentUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "fetchCurrentUser onDataChange: exists=" + snapshot.exists());
                if (snapshot.exists()) {
                    // DIRECT ACCESS OPTIMIZATION: Snapshot is the user node itself
                    mUser = snapshot.getValue(UserModel.class);
                    if (mUser != null) {
                        Log.d(TAG, "fetchCurrentUser: mUser updated: " + mUser.getUsername());
                        if (listener != null) listener.onUserLoaded(mUser);
                    }
                } else {
                    Log.w(TAG, "fetchCurrentUser: User record not found for UID: " + currentUid);
                    if (listener != null) listener.onUserLoaded(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "fetchCurrentUser: onCancelled - " + error.getMessage());
                if (listener != null) listener.onUserLoaded(null);
            }
        };

        // CRITICAL OPTIMIZATION: Listen to specific child directly
        mUsersDatabaseReference.child(currentUid).addValueEventListener(currentUserListener);
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(UserModel user);
    }

    public static void updateUserProfile() {
        updateProfileInfo(mUser, new AuthListener() {
            @Override
            public void onAuthTaskStart() {

            }

            @Override
            public void onAuthSuccess(DataSnapshot snapshot) {

            }

            @Override
            public void onAuthFail(DatabaseError error) {

            }
        });
    }

    public static void updateProfileInfo(UserModel user, AuthListener authListener) {
        authListener.onAuthTaskStart();
        // CRITICAL: Update using UID keys
        mUsersDatabaseReference.child(user.getUid()).setValue(user).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                authListener.onAuthFail(null);
            }
            authListener.onAuthSuccess(null);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (isNightMode()) {
                //icon color -> white
                getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS);
            } else {
                //icon color -> black
                getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS);
            }
        }

        setContentView(R.layout.activity_base);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mUsersDatabaseReference = mDatabase.getReference(Constants.USERS_DB_REF);
        mThreadsDatabaseReference = mDatabase.getReference(Constants.THREADS_DB_REF);
        mFollowRequestsDatabaseReference = mDatabase.getReference("FollowRequests");
        mNotificationsDatabaseReference = mDatabase.getReference("notifications");
        gUsernamesDatabaseReference = mDatabase.getReference(Constants.USERNAMES_DB_REF);

        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Constants.webApplicationID)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(BaseActivity.this, googleSignInOptions);

        if (isUserLoggedIn() && mUser == null) {
            fetchCurrentUser(null);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 22) {
            showProgressDialog();
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (signInAccountTask.isSuccessful()) {
                try {
                    GoogleSignInAccount googleSignInAccount = signInAccountTask.getResult(ApiException.class);
                    if (googleSignInAccount != null) {
                        AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);
                        mAuth.signInWithCredential(authCredential).addOnCompleteListener(BaseActivity.this, task -> {
                            if (!task.isSuccessful()) {
                                Log.e(TAG, "onActivityResult: ", task.getException());
                                showToast("Error: " + task.getException());
                                hideProgressDialog();
                                return;
                            }
                            
                            String uid = task.getResult().getUser().getUid();
                            // Query by UID field, not key
                            mUsersDatabaseReference.orderByChild("uid").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot child : snapshot.getChildren()) {
                                            mUser = child.getValue(UserModel.class);
                                            startActivity(new Intent(BaseActivity.this, MainActivity.class));
                                            finish();
                                            return;
                                        }
                                    }
                                    
                                    // User does not exist, create new one
                                    String email = task.getResult().getUser().getEmail();
                                    String name = task.getResult().getUser().getDisplayName();
                                    String username = name.trim().replace(" ", "_").toLowerCase() + (int)(Math.random() * 1000);
                                    String photoUrl = task.getResult().getUser().getPhotoUrl() != null ? task.getResult().getUser().getPhotoUrl().toString() : "";

                                    UserModel newUser = new UserModel(
                                            new java.util.ArrayList<>(),
                                            new java.util.ArrayList<>(),
                                            "google:google",
                                            new java.util.ArrayList<>(),
                                            true,
                                            photoUrl,
                                            uid,
                                            "google",
                                            new java.util.ArrayList<>(),
                                            new java.util.ArrayList<>(),
                                            name,
                                            true,
                                            new java.util.ArrayList<>(),
                                            email,
                                            new java.util.ArrayList<>(),
                                            username,
                                            ""
                                    );
                                    
                                    
                                    // CRITICAL: Update using UID keys
                                    mUsersDatabaseReference.child(uid).setValue(newUser).addOnCompleteListener(task1 -> {
                                        hideProgressDialog();
                                        if (task1.isSuccessful()) {
                                            mUser = newUser;
                                            startActivity(new Intent(BaseActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            showToast("Failed to create user: " + task1.getException().getMessage());
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    hideProgressDialog();
                                    showToast("Database error: " + error.getMessage());
                                }
                            });
                        });
                    } else {
                        hideProgressDialog();
                    }
                } catch (ApiException e) {
                    hideProgressDialog();
                    Log.e(TAG, "onActivityResult: ", e);
                }
            } else {
                hideProgressDialog();
            }
        }
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public void logoutUser() {
        // CRITICAL: Clear cached user data before signout
        if (currentUserListener != null) {
            mUsersDatabaseReference.removeEventListener(currentUserListener);
            currentUserListener = null;
        }
        mUser = null;
        
        mAuth.signOut();
        try {
            googleSignInClient.signOut();
        } catch (Exception e) {
            Log.e(TAG, "logoutUser(): ", e);
        }
        startActivity(new Intent(this, SplashActivity.class));
        finishAffinity();
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = getProgressDialogInit();
        }
        try {
            progressDialog.show();
        } catch (Exception ignored) {
        }
    }

    public void hideProgressDialog() {
        try {
            progressDialog.dismiss();
            progressDialog = null;
        } catch (Exception ignored) {
        }
    }

    private AlertDialog getProgressDialogInit() {
        return new MaterialAlertDialogBuilder(this).setView(R.layout.progress_dialog).setCancelable(false).setBackground(new ColorDrawable(Color.TRANSPARENT)).create();
    }

    public boolean isNightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public void pressBack(View view) {
        ((Activity) view.getContext()).finish();
        ((Activity) view.getContext()).overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    public void sendPushNotificationInThread(final String type, String token) {
        new Thread(() -> pushNotification(type, token)).start();
    }

    private void pushNotification(String type, String token) {
        JSONObject jPayload = new JSONObject();
        JSONObject jNotification = new JSONObject();
        JSONObject jData = new JSONObject();
        try {
            jNotification.put("title", "Google I/O 2016");
            jNotification.put("body", "Firebase Cloud Messaging (App)");
            jNotification.put("sound", "default");
            jNotification.put("badge", "1");
            jNotification.put("click_action", "OPEN_ACTIVITY_1");
            jNotification.put("icon", "ic_notification");

            jData.put("picture", "https://miro.medium.com/max/1400/1*QyVPcBbT_jENl8TGblk52w.png");

            switch(type) {
                case "tokens":
                    JSONArray ja = new JSONArray();
                    ja.put("c5pBXXsuCN0:APA91bH8nLMt084KpzMrmSWRS2SnKZudyNjtFVxLRG7VFEFk_RgOm-Q5EQr_oOcLbVcCjFH6vIXIyWhST1jdhR8WMatujccY5uy1TE0hkppW_TSnSBiUsH_tRReutEgsmIMmq8fexTmL");
                    ja.put(token);
                    jPayload.put("registration_ids", ja);
                    break;
                case "topic":
                    jPayload.put("to", "/topics/news");
                    break;
                case "condition":
                    jPayload.put("condition", "'sport' in topics || 'news' in topics");
                    break;
                default:
                    jPayload.put("to", token);
            }

            jPayload.put("priority", "high");
            jPayload.put("notification", jNotification);
            jPayload.put("data", jData);

            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", Constants.FCM_AUTH_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send FCM message content.
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(jPayload.toString().getBytes());

            // Read FCM response.
            InputStream inputStream = conn.getInputStream();
            final String resp = convertStreamToString(inputStream);

            Handler h = new Handler(Looper.getMainLooper());
            h.post(() -> Log.i(TAG, "pushNotification: " + resp));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next().replace(",", ",\n") : "";
    }


    public void getUsersDatabase(AuthListener authListener) {
        authListener.onAuthTaskStart();
        mUsersDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                authListener.onAuthSuccess(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                authListener.onAuthFail(error);
            }
        });
    }

    public void loginTask(Task<AuthResult> task) {
        if (task.isSuccessful()) {
            getUsersDatabase(new AuthListener() {
                @Override
                public void onAuthTaskStart() {
                    showProgressDialog();
                }

                @Override
                public void onAuthSuccess(DataSnapshot snapshot) {
                    hideProgressDialog();
                }

                @Override
                public void onAuthFail(DatabaseError error) {
                    hideProgressDialog();
                    showToast(error.getMessage());
                }
            });
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static interface AuthListener {
        void onAuthTaskStart();

        void onAuthSuccess(DataSnapshot snapshot);

        void onAuthFail(DatabaseError error);
    }

}