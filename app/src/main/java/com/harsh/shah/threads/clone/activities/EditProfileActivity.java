package com.harsh.shah.threads.clone.activities;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.databinding.ActivityEditProfileBinding;
import com.harsh.shah.threads.clone.model.UserModel; // Added Import
import com.harsh.shah.threads.clone.utils.Utils;

public class EditProfileActivity extends BaseActivity {

    ActivityEditProfileBinding binding;
    String bio = "", link = ""; // Initialize to empty strings
    boolean isPublicAccount = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- FIX START: Data Recovery Logic ---
        if (mUser == null) {
            FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fUser != null) {
                // Create a temporary user so the app doesn't crash
                mUser = new UserModel();
                mUser.setUid(fUser.getUid());
                // Use email prefix as username (e.g., test@gmail.com -> test)
                String tempUsername = fUser.getEmail() != null ? fUser.getEmail().split("@")[0] : "user";
                mUser.setUsername(tempUsername);
                mUser.setName("New User");
                mUser.setBio("");
                mUser.setInfoLink("");
                mUser.setPublicAccount(true);

                Toast.makeText(this, "Profile missing. Click DONE to fix.", Toast.LENGTH_LONG).show();
            } else {
                // If not logged in, close screen
                finish();
                return;
            }
        }
        // --- FIX END ---

        binding.visibilityLayout.setOnClickListener(v -> binding.switchButton.toggle());
        binding.switchButton.setOnCheckedChangeListener((view, isChecked) -> {
            if (isChecked) {
                binding.textView13.setText("Private profiles can only reply to their followers. Switch to public to reply to anyone.");
            } else {
                binding.textView13.setText("If you switch to private, you won't be able to reply to others unless they follow you.");
            }
        });

        binding.name.setText(String.format("%s ( %s )", mUser.getName(), mUser.getUsername()));

        // FIX: Pre-populate bio, link, and toggle from mUser
        bio = mUser.getBio() != null ? mUser.getBio() : "";
        link = mUser.getInfoLink() != null ? mUser.getInfoLink() : "";
        isPublicAccount = mUser.isPublicAccount();
        
        binding.bio.setText(bio);
        if (!link.isEmpty()) binding.link.setText(link);
        binding.switchButton.setChecked(isPublicAccount);

        binding.done.setOnClickListener(view -> {
            Utils.hideKeyboard(EditProfileActivity.this);

            String newBio = binding.bio.getText().toString().trim();
            String newLink = binding.link.getText().toString().trim();
            boolean newStatus = binding.switchButton.isChecked();

            // Only return if NOTHING changed AND mUser was already valid
            if (newBio.equals(bio.trim()) && newLink.equals(link.trim()) && newStatus == isPublicAccount) {
                // If we just created a recovery user, we MUST continue to save it
                // So we only return if it's NOT a recovery situation
                if (mUser.getUid() != null && !mUser.getUid().isEmpty()) {
                    // return; // Commented out to force save for now
                }
            }

            mUser.setBio(newBio);
            mUser.setInfoLink(newLink);
            mUser.setPublicAccount(newStatus);

            updateProfileInfo(mUser, new AuthListener() {
                @Override
                public void onAuthTaskStart() {
                    showProgressDialog();
                }

                @Override
                public void onAuthSuccess(DataSnapshot snapshot) {
                    hideProgressDialog();
                    Toast.makeText(EditProfileActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onAuthFail(DatabaseError error) {
                    hideProgressDialog();
                    Toast.makeText(EditProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }
}