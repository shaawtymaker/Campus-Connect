package com.harsh.shah.threads.clone.activities.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityFollowingFollowersProfilesBinding;
import com.harsh.shah.threads.clone.model.UserModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FollowingFollowersProfilesActivity extends BaseActivity {

    ActivityFollowingFollowersProfilesBinding binding;
    private ArrayList<String> followerIds = new ArrayList<>();
    private ArrayList<String> followingIds = new ArrayList<>();
    private ArrayList<UserModel> displayedUsers = new ArrayList<>();
    private UsersAdapter adapter;
    private boolean showingFollowers = true; // true = followers, false = following

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFollowingFollowersProfilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter();
        recyclerView.setAdapter(adapter);

        // Load user data first
        fetchCurrentUser(user -> {
            if (user != null) {
                mUser = user;
                binding.username.setText(mUser.getUsername());
                
                // Get followers and following IDs
                followerIds = mUser.getFollowers() != null ? new ArrayList<>(mUser.getFollowers()) : new ArrayList<>();
                followingIds = mUser.getFollowing() != null ? new ArrayList<>(mUser.getFollowing()) : new ArrayList<>();
                
                // Update tab counts
                updateTabCounts();
                
                // Load followers by default
                loadFollowers();
            }
        });

        // Tab selection listener
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Followers tab
                    showingFollowers = true;
                    loadFollowers();
                } else {
                    // Following tab
                    showingFollowers = false;
                    loadFollowing();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Back button
        binding.back.setOnClickListener(v -> finish());
    }

    private void updateTabCounts() {
        TabLayout.Tab followersTab = binding.tabLayout.getTabAt(0);
        TabLayout.Tab followingTab = binding.tabLayout.getTabAt(1);
        
        if (followersTab != null) {
            followersTab.setText("n" + followerIds.size());
        }
        if (followingTab != null) {
            followingTab.setText("Following\n" + followingIds.size());
        }
    }

    private void loadFollowers() {
        displayedUsers.clear();
        adapter.notifyDataSetChanged();
        
        if (followerIds.isEmpty()) {
            return;
        }

        // Load each follower's data from Firebase
        for (String uid : followerIds) {
            mUsersDatabaseReference.orderByChild("uid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            UserModel user = userSnapshot.getValue(UserModel.class);
                            if (user != null && !displayedUsers.contains(user)) {
                                displayedUsers.add(user);
                                adapter.notifyItemInserted(displayedUsers.size() - 1);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }
    }

    private void loadFollowing() {
        displayedUsers.clear();
        adapter.notifyDataSetChanged();
        
        if (followingIds.isEmpty()) {
            return;
        }

        // Load each following user's data from Firebase
        for (String uid : followingIds) {
            mUsersDatabaseReference.orderByChild("uid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            UserModel user = userSnapshot.getValue(UserModel.class);
                            if (user != null && !displayedUsers.contains(user)) {
                                displayedUsers.add(user);
                                adapter.notifyItemInserted(displayedUsers.size() - 1);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }
    }

    // Adapter for displaying users
    class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_frag_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserModel user = displayedUsers.get(position);
            
            holder.username.setText(user.getUsername());
            holder.name.setText(user.getName());
            
            String followersCount = user.getFollowers() != null ? 
                    user.getFollowers().size() + " followers" : "0 followers";
            holder.followers.setText(followersCount);

            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                Picasso.get().load(user.getProfileImage())
                        .placeholder(R.drawable.person_outline_24px)
                        .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.person_outline_24px);
            }

            // Hide follow button (already following/followed)
            holder.followButton.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return displayedUsers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImage;
            TextView username, name, followers, followButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.shapeableImageView);
                username = itemView.findViewById(R.id.username);
                name = itemView.findViewById(R.id.name);
                followers = itemView.findViewById(R.id.followers);
                followButton = itemView.findViewById(R.id.followButton);
            }
        }
    }

    public void pressBack(View view) {
        finish();
    }
}