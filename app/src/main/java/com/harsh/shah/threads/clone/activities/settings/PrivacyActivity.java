package com.harsh.shah.threads.clone.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityPrivacyBinding;

public class PrivacyActivity extends BaseActivity {

    ActivityPrivacyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivacyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.linearLayout8.setOnClickListener(view -> binding.switchButton.toggle());
        binding.peopleYouFollow.setOnClickListener(v -> startActivity(new Intent(PrivacyActivity.this, FollowingFollowersProfilesActivity.class)));
    }
}