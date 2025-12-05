package com.harsh.shah.threads.clone.activities;

import android.os.Bundle;
import android.view.View;

import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.databinding.ActivityUnknownErrorBinding;

public class UnknownErrorActivity extends BaseActivity {

    ActivityUnknownErrorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUnknownErrorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if(getIntent().hasExtra("title")){
            binding.toolbarTitle.setText(getIntent().getStringExtra("title"));
        }

        if(getIntent().hasExtra("desc")){
            binding.description.setText(getIntent().getStringExtra("desc"));
        }

    }

    public void pressBack(View view) {
        finish();
    }
}