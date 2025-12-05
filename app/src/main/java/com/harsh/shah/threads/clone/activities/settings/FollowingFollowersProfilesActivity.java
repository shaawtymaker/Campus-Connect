package com.harsh.shah.threads.clone.activities.settings;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityFollowingFollowersProfilesBinding;

public class FollowingFollowersProfilesActivity extends BaseActivity {

    ActivityFollowingFollowersProfilesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFollowingFollowersProfilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }
}