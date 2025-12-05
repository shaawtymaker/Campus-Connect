package com.harsh.shah.threads.clone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.activities.settings.AccountActivity;
import com.harsh.shah.threads.clone.activities.settings.FollowAndInviteFriendsActivity;
import com.harsh.shah.threads.clone.activities.settings.PrivacyActivity;
import com.harsh.shah.threads.clone.databinding.ActivitySettingsBinding;
import com.harsh.shah.threads.clone.utils.MDialogUtil;

public class SettingsActivity extends BaseActivity {

    ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.followAndInviteFriends.setOnClickListener(v-> startActivity(new Intent(SettingsActivity.this, FollowAndInviteFriendsActivity.class)));
        binding.notifications.setOnClickListener(view -> startActivity(new Intent(SettingsActivity.this, UnknownErrorActivity.class)));
        binding.privacy.setOnClickListener(view -> startActivity(new Intent(SettingsActivity.this, PrivacyActivity.class)));
        binding.account.setOnClickListener(view -> startActivity(new Intent(SettingsActivity.this, AccountActivity.class)));
        binding.language.setOnClickListener(view -> startActivity(new Intent(SettingsActivity.this, UnknownErrorActivity.class).putExtra("desc", "Coming soon!").putExtra("title","Language")));

        binding.help.setOnClickListener(view -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(SettingsActivity.this);
            bottomSheetDialog.setContentView(R.layout.layout_help);
            bottomSheetDialog.show();
        });

        binding.logout.setOnClickListener(v->{
            MDialogUtil mDialogUtil =
            new MDialogUtil(SettingsActivity.this)
                    .setTitle("Log out Threads?")
                    .setMessage("are you sure you want to logout?",false)
                    .setB1("Logout", view -> logoutUser());
            AlertDialog dialog = mDialogUtil.create();
            mDialogUtil.setB2("Cancel", view -> dialog.dismiss());
            dialog.show();
        });
    }
}