package com.harsh.shah.threads.clone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.Constants;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.activities.settings.FollowingFollowersProfilesActivity;
import com.harsh.shah.threads.clone.databinding.ActivityProfileBinding;

public class ProfileActivity extends BaseActivity {

    ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.logout.setOnClickListener(view -> {
            logoutUser();
            startActivity(new Intent(this, SplashActivity.class));
            finishAffinity();
        });



        binding.next.setOnClickListener(view -> startActivity(new Intent(ProfileActivity.this, MainActivity.class)));
        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
    }
}