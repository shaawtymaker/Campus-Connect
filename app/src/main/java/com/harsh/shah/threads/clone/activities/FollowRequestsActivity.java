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
                .addValueEventListener(new ValueEventListener() {
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
        // Add requester to user's followers list
        if (mUser.getFollowers() == null) mUser.setFollowers(new ArrayList<>());
        if (!mUser.getFollowers().contains(request.getRequesterId())) {
            mUser.getFollowers().add(request.getRequesterId());
        }
        
        // Update current user in Firebase
        mUsersDatabaseReference.child(mUser.getUsername()).setValue(mUser);
        
        // Add current user to requester's following list
        mUsersDatabaseReference.child(request.getRequesterUsername())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserModel requester = snapshot.getValue(UserModel.class);
                        if (requester != null) {
                            if (requester.getFollowing() == null) requester.setFollowing(new ArrayList<>());
                            if (!requester.getFollowing().contains(mUser.getUid())) {
                                requester.getFollowing().add(mUser.getUid());
                            }
                            mUsersDatabaseReference.child(request.getRequesterUsername()).setValue(requester);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error updating requester: " + error.getMessage());
                    }
                });
        
        // Update request status to accepted
        request.setStatus("accepted");
        mFollowRequestsDatabaseReference.child(mUser.getUid())
                .child(request.getRequestId())
                .setValue(request);
        
        showToast("Follow request accepted");
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
