package com.harsh.shah.threads.clone.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.interfaces.profile.onProfileUpdate;
import com.harsh.shah.threads.clone.interfaces.profile.onProfileUpdateImpl;
import com.harsh.shah.threads.clone.model.UserModel;

public class SplashActivity extends BaseActivity implements onProfileUpdate {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        nextScreen();

//        StorageDatabase.INSTANCE.uploadFile(InputFile.Companion.fromBytes(new byte[]{},"temp","image/png"));
//        StorageDatabase.INSTANCE.downloadFile("672b82950007fbde1497");

    }

    private void nextScreen(){
        new Handler().postDelayed(() -> {
            Pair<View, String> p1 = Pair.create(findViewById(R.id.imageView), "splash_image");
            Intent intent = new Intent(SplashActivity.this, isUserLoggedIn() ? MainActivity.class : AuthActivity.class);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(SplashActivity.this, p1);
            startActivity(intent, options.toBundle());
        }, 3000);

        new Handler().postDelayed(this::finish, 6000);
    }

    @Override
    public void setup() {

    }

    @Override
    public void onProfileUpdate(UserModel userModel) {

    }
}