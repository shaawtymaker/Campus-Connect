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
        
        // Add requester to user's followers list
        if (mUser.getFollowers() == null) mUser.setFollowers(new ArrayList<>());
        if (!mUser.getFollowers().contains(request.getRequesterId())) {
            mUser.getFollowers().add(request.getRequesterId());
        }
        
        // CRITICAL FIX: Use UID as key
        mUsersDatabaseReference.child(mUser.getUid()).setValue(mUser)
                .addOnSuccessListener(aVoid -> {
                    updateRequesterFollowingList(request);
                })
                .addOnFailureListener(e -> {
                    showToast("Error accepting request");
                    if (mUser.getFollowers() != null) {
                        mUser.getFollowers().remove(request.getRequesterId());
                    }
                });
    }
    
    private void updateRequesterFollowingList(FollowRequestModel request) {
        // Direct access by UID - no need to query
        mUsersDatabaseReference.child(request.getRequesterId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserModel requester = snapshot.getValue(UserModel.class);
                        if (requester != null) {
                            if (requester.getFollowing() == null) requester.setFollowing(new ArrayList<>());
                            if (!requester.getFollowing().contains(mUser.getUid())) {
                                requester.getFollowing().add(mUser.getUid());
                            }
                            
                            // CRITICAL FIX: Use UID as key
                            mUsersDatabaseReference.child(requester.getUid()).setValue(requester)
                                    .addOnSuccessListener(aVoid -> {
                                        removeFollowRequest(request);
                                    })
                                    .addOnFailureListener(e -> {
                                        showToast("Error completing follow request");
                                        // Rollback local changes manually if needed
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Error fetching requester data");
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
                    
                    // Send notification to requester
                    String notificationId = mNotificationsDatabaseReference.push().getKey();
                    com.harsh.shah.threads.clone.model.NotificationModel notification = new com.harsh.shah.threads.clone.model.NotificationModel(
                        notificationId,
                        "FOLLOW_ACCEPTED",
                        mUser.getUid(),
                        com.harsh.shah.threads.clone.utils.Utils.getNowInMillis() + "",
                        "",
                        "accepted your follow request"
                    );
                    mNotificationsDatabaseReference.child(request.getRequesterId()).child(notificationId).setValue(notification);
                    
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
            
            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(v.getContext(), com.harsh.shah.threads.clone.activities.OtherUserProfileActivity.class);
                intent.putExtra("uid", request.getRequesterId());
                v.getContext().startActivity(intent);
            });
            
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
