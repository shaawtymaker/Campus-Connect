package com.harsh.shah.threads.clone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityOtherUserProfileBinding;
import com.harsh.shah.threads.clone.fragments.ProfileThreadsFragment;
import com.harsh.shah.threads.clone.model.FollowRequestModel;
import com.harsh.shah.threads.clone.model.NotificationModel;
import com.harsh.shah.threads.clone.model.UserModel;
import com.harsh.shah.threads.clone.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class OtherUserProfileActivity extends BaseActivity {

    private ActivityOtherUserProfileBinding binding;
    private String targetUserId;
    private UserModel targetUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtherUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        targetUserId = getIntent().getStringExtra("uid");
        if (targetUserId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.backButton.setOnClickListener(v -> finish());
        binding.moreOptions.setOnClickListener(v -> {
            // Future: Implement Block/Report
            Toast.makeText(this, "More options coming soon", Toast.LENGTH_SHORT).show();
        });

        fetchTargetUser();
    }

    private void fetchTargetUser() {
        // Query by UID to find the user, then use their object
        mUsersDatabaseReference.orderByChild("uid").equalTo(targetUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        targetUser = child.getValue(UserModel.class);
                        if (targetUser != null) {
                            updateUI();
                            setupTabs();
                            checkFollowStatus();
                        }
                    }
                } else {
                    Toast.makeText(OtherUserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OtherUserProfileActivity.this, "Error fetching user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        binding.name.setText(targetUser.getName());
        binding.username.setText(targetUser.getUsername());
        binding.bio.setText(targetUser.getBio());
        
        if (targetUser.getInfoLink() != null && !targetUser.getInfoLink().isEmpty()) {
            binding.addedLink.setText("• " + targetUser.getInfoLink());
            binding.addedLink.setVisibility(View.VISIBLE);
        } else {
            binding.addedLink.setVisibility(View.GONE);
        }

        String followersCount = targetUser.getFollowers() != null ? targetUser.getFollowers().size() + " followers" : "0 followers";
        binding.followers.setText(followersCount);

        if (targetUser.getProfileImage() != null && !targetUser.getProfileImage().isEmpty()) {
            Picasso.get().load(targetUser.getProfileImage()).placeholder(R.drawable.person_outline_24px).into(binding.profileImage);
        }
    }

    private void setupTabs() {
        binding.viewPager.setAdapter(new PageAdapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText(position == 0 ? "Threads" : position == 1 ? "Replies" : "Reposts")).attach();
        
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position));
            }
        });
    }

    private void checkFollowStatus() {
        if (mUser == null) {
            fetchCurrentUser(user -> {
                if (user != null) {
                    mUser = user;
                    checkFollowStatus(); 
                }
            });
            return;
        }

        // Check if already following
        if (mUser.getFollowing() != null && mUser.getFollowing().contains(targetUser.getUid())) {
            setFollowButtonState("Following");
            return;
        }

        // Check if requested
        mFollowRequestsDatabaseReference.child(targetUser.getUid())
                .orderByChild("requesterId")
                .equalTo(mUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            setFollowButtonState("Requested");
                        } else {
                            setFollowButtonState("Follow");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setFollowButtonState("Follow");
                    }
                });
    }

    private void setFollowButtonState(String state) {
        binding.followButton.setText(state);
        if ("Following".equals(state) || "Requested".equals(state)) {
            binding.followButton.setBackgroundResource(R.drawable.button_background_outlined);
            binding.followButton.setTextColor(getResources().getColor(R.color.textSec));
        } else {
            binding.followButton.setBackgroundResource(R.drawable.button_background);
            binding.followButton.setTextColor(getResources().getColor(R.color.textMain));
        }

        binding.followButton.setOnClickListener(v -> handleFollowClick(state));
    }

    private void handleFollowClick(String currentState) {
        if ("Following".equals(currentState)) {
            unfollowUser();
        } else if ("Requested".equals(currentState)) {
            cancelFollowRequest();
        } else {
            if (targetUser.isPublicAccount()) {
                followUser();
            } else {
                sendFollowRequest();
            }
        }
    }

    private void followUser() {
        if (mUser.getFollowing() == null) mUser.setFollowing(new ArrayList<>());
        if (targetUser.getFollowers() == null) targetUser.setFollowers(new ArrayList<>());

        boolean changed = false;
        if (!mUser.getFollowing().contains(targetUser.getUid())) {
            mUser.getFollowing().add(targetUser.getUid());
            changed = true;
        }
        if (!targetUser.getFollowers().contains(mUser.getUid())) {
            targetUser.getFollowers().add(mUser.getUid());
            changed = true;
        }

        if (changed) {
            binding.followButton.setEnabled(false); // Disable to prevent double clicks
            
            // CRITICAL: Update using UID keys
            mUsersDatabaseReference.child(mUser.getUid()).setValue(mUser);
            mUsersDatabaseReference.child(targetUser.getUid()).setValue(targetUser)
                .addOnCompleteListener(task -> {
                    binding.followButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        setFollowButtonState("Following");
                        sendFollowNotification();
                        
                        // Update UI explicitly 
                        if (targetUser.getFollowers() != null) {
                            String count = targetUser.getFollowers().size() + " followers";
                            binding.followers.setText(count);
                        }
                    } else {
                        Toast.makeText(OtherUserProfileActivity.this, "Failed to follow", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void unfollowUser() {
        if (mUser.getFollowing() != null) mUser.getFollowing().remove(targetUser.getUid());
        if (targetUser.getFollowers() != null) targetUser.getFollowers().remove(mUser.getUid());

        mUsersDatabaseReference.child(mUser.getUid()).setValue(mUser);
        mUsersDatabaseReference.child(targetUser.getUid()).setValue(targetUser)
            .addOnSuccessListener(aVoid -> setFollowButtonState("Follow"));
    }

    private void sendFollowRequest() {
        String requestId = mFollowRequestsDatabaseReference.child(targetUser.getUid()).push().getKey();
        FollowRequestModel request = new FollowRequestModel(
                requestId,
                mUser.getUid(),
                mUser.getUsername(),
                mUser.getName(),
                mUser.getProfileImage(),
                targetUser.getUid(),
                Utils.getNowInMillis() + "",
                "pending"
        );

        mFollowRequestsDatabaseReference.child(targetUser.getUid()).child(requestId).setValue(request)
                .addOnSuccessListener(aVoid -> setFollowButtonState("Requested"));
    }

    private void cancelFollowRequest() {
        mFollowRequestsDatabaseReference.child(targetUser.getUid())
                .orderByChild("requesterId")
                .equalTo(mUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                        }
                        setFollowButtonState("Follow");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendFollowNotification() {
        // Create notification for the target user
        String notificationId = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("notifications").push().getKey();
        NotificationModel notification = new NotificationModel(
            notificationId,
            "FOLLOW",
            mUser.getUid(),
            Utils.getNowInMillis() + "",
            "", // No thread ID
            "started following you"
        );
        
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("notifications")
            .child(targetUser.getUid())
            .child(notificationId)
            .setValue(notification);
    }

    class PageAdapter extends FragmentStateAdapter {
        public PageAdapter(@NonNull BaseActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            // Reuse ProfileThreadsFragment but we need to tell it to fetch for THIS user, not current user
            // We need to modify ProfileThreadsFragment to accept a userId
            return ProfileThreadsFragment.newInstance(
                position == 0 ? ProfileThreadsFragment.MODE_THREADS : position == 1 ? ProfileThreadsFragment.MODE_REPLIES : ProfileThreadsFragment.MODE_REPOSTS,
                targetUserId 
            );
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
