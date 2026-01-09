package com.harsh.shah.threads.clone.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.model.FollowRequestModel;
import com.harsh.shah.threads.clone.model.UserModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class FollowRequestsActivity extends BaseActivity {

    private static final String TAG = "FollowRequestsActivity";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private RequestsAdapter adapter;
    private List<FollowRequestModel> requests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_requests);

        recyclerView = findViewById(R.id.requestsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        adapter = new RequestsAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadFollowRequests();
    }

    private void loadFollowRequests() {
        if (mUser == null) {
            showToast("Please wait, loading user data...");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        mFollowRequestsDatabaseReference.child(mUser.getUid())
                .orderByChild("status")
                .equalTo("pending")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requests.clear();
                        
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            FollowRequestModel request = requestSnapshot.getValue(FollowRequestModel.class);
                            if (request != null) {
                                requests.add(request);
                            }
                        }
                        
                        progressBar.setVisibility(View.GONE);
                        
                        if (requests.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                        
                        Log.d(TAG, "Loaded " + requests.size() + " follow requests");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error loading requests: " + error.getMessage());
                        showToast("Error loading requests");
                    }
                });
    }

    private void acceptRequest(FollowRequestModel request) {
        Log.d(TAG, "=== ACCEPT REQUEST CALLED ===");
        Log.d(TAG, "Current User UID: " + mUser.getUid());
        Log.d(TAG, "Requester UID: " + request.getRequesterId());
        
        // Add requester to user's followers list
        if (mUser.getFollowers() == null) mUser.setFollowers(new ArrayList<>());
        if (!mUser.getFollowers().contains(request.getRequesterId())) {
            mUser.getFollowers().add(request.getRequesterId());
            Log.d(TAG, "Added requester to local followers list. Total followers: " + mUser.getFollowers().size());
        } else {
            Log.d(TAG, "Requester already in followers list");
        }
        
        // FIX: Update current user in Firebase using username (key), not UID
        Log.d(TAG, "Updating current user in Firebase...");
        mUsersDatabaseReference.child(mUser.getUsername()).setValue(mUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Current user updated successfully in Firebase");
                    // Only proceed to update requester after current user is updated
                    updateRequesterFollowingList(request);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Error updating current user: " + e.getMessage());
                    showToast("Error accepting request");
                    // Rollback: Remove from local list
                    if (mUser.getFollowers() != null) {
                        mUser.getFollowers().remove(request.getRequesterId());
                    }
                });
    }
    
    
    private void updateRequesterFollowingList(FollowRequestModel request) {
        Log.d(TAG, "Fetching requester from Firebase...");
        // Add current user to requester's following list
        // FIX: Use query to find requester by UID, then use their username as key for update
        mUsersDatabaseReference.orderByChild("uid").equalTo(request.getRequesterId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Requester data fetched");
                        UserModel requester = snapshot.getValue(UserModel.class);
                        if (requester != null) {
                            Log.d(TAG, "Requester username: " + requester.getUsername());
                            if (requester.getFollowing() == null) requester.setFollowing(new ArrayList<>());
                            if (!requester.getFollowing().contains(mUser.getUid())) {
                                requester.getFollowing().add(mUser.getUid());
                                Log.d(TAG, "Added current user to requester's following. Total following: " + requester.getFollowing().size());
                            } else {
                                Log.d(TAG, "Current user already in requester's following");
                            }
                            // FIX: Use username as key for update
                            Log.d(TAG, "Updating requester in Firebase...");
                            mUsersDatabaseReference.child(requester.getUsername()).setValue(requester)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✓ Requester updated successfully in Firebase");
                                        // SUCCESS: Both updates complete, now remove the request
                                        removeFollowRequest(request);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "✗ Error updating requester: " + e.getMessage());
                                        showToast("Error completing follow request");
                                        // Rollback both users
                                        if (mUser.getFollowers() != null) {
                                            mUser.getFollowers().remove(request.getRequesterId());
                                            mUsersDatabaseReference.child(mUser.getUid()).setValue(mUser);
                                        }
                                    });
                        } else {
                            Log.e(TAG, "✗ Requester is null!");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "✗ Error fetching requester: " + error.getMessage());
                        showToast("Error completing follow request");
                        // Rollback current user
                        if (mUser.getFollowers() != null) {
                            mUser.getFollowers().remove(request.getRequesterId());
                            mUsersDatabaseReference.child(mUser.getUid()).setValue(mUser);
                        }
                    }
                });
    }
    
    
    private void removeFollowRequest(FollowRequestModel request) {
        Log.d(TAG, "Removing follow request from Firebase...");
        // Remove request from Firebase
        mFollowRequestsDatabaseReference.child(mUser.getUid())
                .child(request.getRequestId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Follow request removed successfully");
                    // Remove from local list and refresh UI
                    requests.remove(request);
                    adapter.notifyDataSetChanged();
                    
                    // Show/hide empty view
                    if (requests.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                    
                    showToast("Follow request accepted");
                    Log.d(TAG, "Follow request accepted successfully");
                    
                    // FIX: Refresh mUser to reflect updated followers list
                    // Use forceRefresh=true to bypass cache and get fresh data from Firebase
                    fetchCurrentUser(true, user -> {
                        if (user != null) {
                            mUser = user;
                            Log.d(TAG, "✓ mUser refreshed with forceRefresh, followers: " + mUser.getFollowers().size());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing request: " + e.getMessage());
                    // Request removal failed but follower relationship is established
                    showToast("Follow request accepted (pending cleanup)");
                });
    }

    private void rejectRequest(FollowRequestModel request) {
        // Update request status to rejected
        request.setStatus("rejected");
        mFollowRequestsDatabaseReference.child(mUser.getUid())
                .child(request.getRequestId())
                .setValue(request)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Remove the request after marking as rejected
                        mFollowRequestsDatabaseReference.child(mUser.getUid())
                                .child(request.getRequestId())
                                .removeValue();
                        showToast("Follow request rejected");
                    }
                });
    }

    class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.follow_request_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FollowRequestModel request = requests.get(position);
            
            holder.username.setText(request.getRequesterUsername());
            holder.name.setText(request.getRequesterName());
            
            // Load follower count (can be enhanced to fetch from Firebase)
            holder.followers.setText("Wants to follow you");
            
            if (request.getRequesterProfileImage() != null && !request.getRequesterProfileImage().isEmpty()) {
                Picasso.get().load(request.getRequesterProfileImage())
                        .placeholder(R.drawable.person_outline_24px)
                        .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.person_outline_24px);
            }
            
            holder.acceptButton.setOnClickListener(v -> {
                acceptRequest(request);
            });
            
            holder.rejectButton.setOnClickListener(v -> {
                rejectRequest(request);
            });
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView profileImage;
            TextView username, name, followers;
            TextView acceptButton, rejectButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profileImage);
                username = itemView.findViewById(R.id.username);
                name = itemView.findViewById(R.id.name);
                followers = itemView.findViewById(R.id.followers);
                acceptButton = itemView.findViewById(R.id.acceptButton);
                rejectButton = itemView.findViewById(R.id.rejectButton);
            }
        }
    }
}
