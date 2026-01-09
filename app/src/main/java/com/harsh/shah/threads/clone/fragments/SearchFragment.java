package com.harsh.shah.threads.clone.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.harsh.shah.threads.clone.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private static SearchFragment instance;
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private Adapter adapter;
    private List<UserModel> allUsers = new ArrayList<>();
    private List<UserModel> filteredUsers = new ArrayList<>();

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    public static SearchFragment getInstance() {
        if (instance == null) {
            instance = new SearchFragment();
        }
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        searchEditText = view.findViewById(R.id.searchEditText);
        recyclerView = view.findViewById(R.id.recyclerView);
        
        adapter = new Adapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fetchAllUsers();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchAllUsers() {
        BaseActivity.mUsersDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsers.clear();
                String currentUid = BaseActivity.mUser != null ? BaseActivity.mUser.getUid() : null;
                
                java.util.Set<String> addedUids = new java.util.HashSet<>();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    UserModel user = dataSnapshot.getValue(UserModel.class);
                    if (user != null) {
                        // Only filter out the current user if we know who it is
                        if (currentUid == null || !user.getUid().equals(currentUid)) {
                            // Deduplication logic: Check if UID already added
                            if (user.getUid() != null && !addedUids.contains(user.getUid())) {
                                allUsers.add(user);
                                addedUids.add(user.getUid());
                            } else if (user.getUid() == null) {
                                // Fallback for legacy users without UID (though they shouldn't exist ideally)
                                if (!allUsers.contains(user)) {
                                     allUsers.add(user);
                                }
                            }
                        }
                    }
                }
                filter(searchEditText.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void filter(String query) {
        filteredUsers.clear();
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            for (UserModel user : allUsers) {
                if ((user.getUsername() != null && user.getUsername().toLowerCase().contains(query.toLowerCase())) ||
                    (user.getName() != null && user.getName().toLowerCase().contains(query.toLowerCase()))) {
                    filteredUsers.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Helper method to check if a follow request is pending
    private void checkFollowRequestStatus(String targetUserId, TextView button) {
        BaseActivity.mFollowRequestsDatabaseReference
            .child(targetUserId)
            .orderByChild("requesterId")
            .equalTo(BaseActivity.mUser.getUid())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        button.setText("Requested");
                        button.setBackgroundResource(R.drawable.button_background_outlined);
                        button.setTextColor(getResources().getColor(R.color.textSec));
                    } else {
                        button.setText("Follow");
                        button.setBackgroundResource(R.drawable.button_background);
                        button.setTextColor(getResources().getColor(R.color.textMain));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Default to Follow on error
                    button.setText("Follow");
                    button.setBackgroundResource(R.drawable.button_background);
                    button.setTextColor(getResources().getColor(R.color.textMain));
                }
            });
    }

    // Helper method to cancel a pending follow request
    private void cancelFollowRequest(String targetUserId, TextView button) {
        BaseActivity.mFollowRequestsDatabaseReference
            .child(targetUserId)
            .orderByChild("requesterId")
            .equalTo(BaseActivity.mUser.getUid())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                        requestSnapshot.getRef().removeValue();
                    }
                    button.setText("Follow");
                    button.setBackgroundResource(R.drawable.button_background);
                    button.setTextColor(getResources().getColor(R.color.textMain));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    ((BaseActivity) getActivity()).showToast("Error canceling request");
                }
            });
    }

    // Helper method to check and send follow request (prevents duplicates)
    private void checkAndSendFollowRequest(UserModel user, TextView button, int position) {
        // First check if request already exists
        BaseActivity.mFollowRequestsDatabaseReference
            .child(user.getUid())
            .orderByChild("requesterId")
            .equalTo(BaseActivity.mUser.getUid())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Request already exists
                        ((BaseActivity) getActivity()).showToast("Request already sent");
                        button.setText("Requested");
                        button.setBackgroundResource(R.drawable.button_background_outlined);
                        button.setTextColor(getResources().getColor(R.color.textSec));
                    } else {
                        // Send new request
                        String requestId = BaseActivity.mFollowRequestsDatabaseReference
                            .child(user.getUid()).push().getKey();
                        
                        FollowRequestModel request = new FollowRequestModel(
                            requestId,
                            BaseActivity.mUser.getUid(),
                            BaseActivity.mUser.getUsername(),
                            BaseActivity.mUser.getName(),
                            BaseActivity.mUser.getProfileImage(),
                            user.getUid(),
                            Utils.getNowInMillis() + "",
                            "pending"
                        );
                        
                        BaseActivity.mFollowRequestsDatabaseReference
                            .child(user.getUid())
                            .child(requestId)
                            .setValue(request);
                        
                        button.setText("Requested");
                        button.setBackgroundResource(R.drawable.button_background_outlined);
                        button.setTextColor(getResources().getColor(R.color.textSec));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    ((BaseActivity) getActivity()).showToast("Error sending request");
                }
            });
    }

    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.search_frag_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserModel user = filteredUsers.get(position);
            
            holder.username.setText(user.getUsername());
            holder.name.setText(user.getName());
            
            String followersCount = user.getFollowers() != null ? user.getFollowers().size() + " followers" : "0 followers";
            holder.followers.setText(followersCount);

            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                Picasso.get().load(user.getProfileImage()).placeholder(R.drawable.person_outline_24px).into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.person_outline_24px);
            }

            // FIX: Check button state - Following, Requested, or Follow
            if (BaseActivity.mUser != null && BaseActivity.mUser.getFollowing() != null && 
                BaseActivity.mUser.getFollowing().contains(user.getUid())) {
                holder.followButton.setText("Following");
                holder.followButton.setBackgroundResource(R.drawable.button_background_outlined);
                holder.followButton.setTextColor(getResources().getColor(R.color.textSec));
            } else {
                // FIX: Check if follow request is pending
                checkFollowRequestStatus(user.getUid(), holder.followButton);
            }

            holder.followButton.setOnClickListener(v -> {
                if (BaseActivity.mUser == null) {
                    ((BaseActivity) getActivity()).showToast("Loading user profile, please wait...");
                    return;
                }
                
                String buttonText = holder.followButton.getText().toString();
                
                if (buttonText.equals("Following")) {
                    // Unfollow - Fetch target user from Firebase first to ensure we have latest data
                    BaseActivity.mUsersDatabaseReference.orderByChild("uid").equalTo(user.getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                        UserModel targetUser = userSnapshot.getValue(UserModel.class);
                                        if (targetUser != null) {
                                            // Remove from current user's following list
                                            if (BaseActivity.mUser.getFollowing() != null) {
                                                BaseActivity.mUser.getFollowing().remove(targetUser.getUid());
                                            }
                                            
                                            // Remove from target user's followers list
                                            if (targetUser.getFollowers() != null) {
                                                targetUser.getFollowers().remove(BaseActivity.mUser.getUid());
                                            }
                                            
                                            // Update both users in Firebase using their username as key
                                            if (BaseActivity.mUser.getUsername() != null && targetUser.getUsername() != null) {
                                                BaseActivity.mUsersDatabaseReference.child(BaseActivity.mUser.getUsername())
                                                    .setValue(BaseActivity.mUser);
                                                BaseActivity.mUsersDatabaseReference.child(targetUser.getUsername())
                                                    .setValue(targetUser);
                                                
                                                // Update UI
                                                holder.followButton.setText("Follow");
                                                holder.followButton.setBackgroundResource(R.drawable.button_background);
                                                holder.followButton.setTextColor(getResources().getColor(R.color.textMain));
                                                
                                                ((BaseActivity) getActivity()).showToast("Unfollowed " + targetUser.getUsername());
                                            } else {
                                                ((BaseActivity) getActivity()).showToast("Error update: username missing");
                                            }
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                ((BaseActivity) getActivity()).showToast("Error unfollowing user");
                            }
                        });
                } else if (buttonText.equals("Requested")) {
                    // Cancel pending request
                    cancelFollowRequest(user.getUid(), holder.followButton);
                } else { // "Follow"
                    // Check if account is private
                    if (!user.isPublicAccount()) {
                        // Send follow request for private accounts
                        checkAndSendFollowRequest(user, holder.followButton, position);
                    } else {
                        // Public account - direct follow - Fetch target user from Firebase first
                        BaseActivity.mUsersDatabaseReference.orderByChild("uid").equalTo(user.getUid())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                            UserModel targetUser = userSnapshot.getValue(UserModel.class);
                                            if (targetUser != null) {
                                                // Initialize lists if null
                                                if (BaseActivity.mUser.getFollowing() == null) {
                                                    BaseActivity.mUser.setFollowing(new ArrayList<>());
                                                }
                                                if (targetUser.getFollowers() == null) {
                                                    targetUser.setFollowers(new ArrayList<>());
                                                }
                                                
                                                // Check if not already following
                                                if (!BaseActivity.mUser.getFollowing().contains(targetUser.getUid())) {
                                                    BaseActivity.mUser.getFollowing().add(targetUser.getUid());
                                                    targetUser.getFollowers().add(BaseActivity.mUser.getUid());
                                                    
                                                    // Update both users in Firebase using their username as key
                                                    if (BaseActivity.mUser.getUsername() != null && targetUser.getUsername() != null) {
                                                        BaseActivity.mUsersDatabaseReference.child(BaseActivity.mUser.getUsername())
                                                            .setValue(BaseActivity.mUser);
                                                        BaseActivity.mUsersDatabaseReference.child(targetUser.getUsername())
                                                            .setValue(targetUser);
                                                        
                                                        // Update UI
                                                        holder.followButton.setText("Following");
                                                        holder.followButton.setBackgroundResource(R.drawable.button_background_outlined);
                                                        holder.followButton.setTextColor(getResources().getColor(R.color.textSec));
                                                        
                                                        ((BaseActivity) getActivity()).showToast("Following " + targetUser.getUsername());
                                                    } else {
                                                        ((BaseActivity) getActivity()).showToast("Error follow: username missing");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    ((BaseActivity) getActivity()).showToast("Error following user");
                                }
                            });
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredUsers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView username, name, followers, followButton;
            ShapeableImageView profileImage;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                name = itemView.findViewById(R.id.name);
                followers = itemView.findViewById(R.id.followers);
                followButton = itemView.findViewById(R.id.followButton); // Note: ID in xml is just a TextView without ID, I need to check xml again.
                profileImage = itemView.findViewById(R.id.shapeableImageView);
                
                // The follow button in XML didn't have an ID in the previous view_file. 
                // I need to check if I need to add an ID to the follow button in XML.
                // Looking at previous view_file of search_frag_list_item.xml, the follow button TextView DOES NOT have an ID.
                // It is just <TextView ... text="Follow" ... />
                // I must add an ID to it in XML first or find it by tag/index. Adding ID is better.
                // For now I will assume I will add the ID "followButton".
                followButton = (TextView) itemView.findViewById(R.id.followButton);
            }
        }
    }
}