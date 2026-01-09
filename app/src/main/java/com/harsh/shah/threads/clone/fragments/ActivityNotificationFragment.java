
package com.harsh.shah.threads.clone.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.model.NotificationModel;
import com.harsh.shah.threads.clone.model.UserModel;
import com.harsh.shah.threads.clone.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ActivityNotificationFragment extends Fragment {

    private static ActivityNotificationFragment instance;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    TextView chip_all, chip_requests, chip_replies, chip_mentions, chip_quotes, chip_reposts, no_data_text;
    RecyclerView recyclerView;
    int selectedPosition = 0;
    
    List<NotificationModel> allNotifications = new ArrayList<>();
    List<NotificationModel> filteredNotifications = new ArrayList<>();
    DataAdapter adapter;

    public ActivityNotificationFragment() {
        // Required empty public constructor
    }

    public static ActivityNotificationFragment newInstance(String param1, String param2) {
        ActivityNotificationFragment fragment = new ActivityNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static ActivityNotificationFragment getInstance() {
        if (instance == null) {
            instance = new ActivityNotificationFragment();
        }
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chip_all = view.findViewById(R.id.chip_all);
        chip_requests = view.findViewById(R.id.chip_requests);
        chip_replies = view.findViewById(R.id.chip_replies);
        chip_mentions = view.findViewById(R.id.chip_mentions);
        chip_quotes = view.findViewById(R.id.chip_quotes);
        chip_reposts = view.findViewById(R.id.chip_reposts);

        no_data_text = view.findViewById(R.id.no_data_text);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new DataAdapter();
        recyclerView.setAdapter(adapter);

        chip_all.setOnClickListener(view1 -> setPosition(0));
        chip_requests.setOnClickListener(view1 -> {
            // Open FollowRequestsActivity
            Intent intent = new Intent(getActivity(), com.harsh.shah.threads.clone.activities.FollowRequestsActivity.class);
            startActivity(intent);
        });
        chip_replies.setOnClickListener(view1 -> setPosition(2));
        chip_mentions.setOnClickListener(view1 -> setPosition(3));
        chip_quotes.setOnClickListener(view1 -> setPosition(4));
        chip_reposts.setOnClickListener(view1 -> setPosition(5));

        setPosition(0);
        fetchNotifications();
        loadFollowRequestsCount();
    }
    
    private void loadFollowRequestsCount() {
        if (BaseActivity.mUser == null) return;
        
        BaseActivity.mFollowRequestsDatabaseReference.child(BaseActivity.mUser.getUid())
                .orderByChild("status")
                .equalTo("pending")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (count > 0) {
                            chip_requests.setText("Requests (" + count + ")");
                        } else {
                            chip_requests.setText("Requests");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchNotifications() {
        if (BaseActivity.mUser == null) return;

        // Assuming notifications are stored at "notifications/{userId}"
        // Since BaseActivity.mThreadsDatabaseReference points to "threads", we need a new reference or use FirebaseDatabase.getInstance()
        // I'll use FirebaseDatabase.getInstance().getReference("notifications")
        
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("notifications")
                .child(BaseActivity.mUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allNotifications.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            NotificationModel model = data.getValue(NotificationModel.class);
                            if (model != null) {
                                allNotifications.add(0, model); // Add to top
                            }
                        }
                        filterNotifications();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void setPosition(int position) {
        setHeaderPos(chip_all, position == 0);
        setHeaderPos(chip_requests, position == 1);
        setHeaderPos(chip_replies, position == 2);
        setHeaderPos(chip_mentions, position == 3);
        setHeaderPos(chip_quotes, position == 4);
        setHeaderPos(chip_reposts, position == 5);
        selectedPosition = position;
        filterNotifications();
    }

    private void filterNotifications() {
        filteredNotifications.clear();
        if (selectedPosition == 0) {
            filteredNotifications.addAll(allNotifications);
        } else {
            String type = "";
            switch (selectedPosition) {
                case 1: type = "FOLLOW"; break;
                case 2: type = "REPLY"; break;
                case 3: type = "MENTION"; break;
                case 4: type = "QUOTE"; break;
                case 5: type = "REPOST"; break;
            }
            for (NotificationModel model : allNotifications) {
                if (model.getType() != null && model.getType().equalsIgnoreCase(type)) {
                    filteredNotifications.add(model);
                }
            }
        }

        if (filteredNotifications.isEmpty()) {
            no_data_text.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            no_data_text.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void setHeaderPos(TextView view, boolean isActivated) {
        if (isActivated) {
            TextViewCompat.setTextAppearance(view, R.style.Base_Widget_AppCompat_TextView_ButtonFilled);
            view.setBackgroundResource(R.drawable.button_background_filled);
        } else {
            TextViewCompat.setTextAppearance(view, R.style.Base_Widget_AppCompat_TextView_ButtonOutlined);
            view.setBackgroundResource(R.drawable.button_background_outlined);
        }
    }

    class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_activity_follow_req_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationModel model = filteredNotifications.get(position);
            
            // Set message based on type
            String message = "";
            if ("FOLLOW".equalsIgnoreCase(model.getType())) {
                message = "Followed you";
                holder.followButton.setVisibility(View.VISIBLE);
            } else if ("LIKE".equalsIgnoreCase(model.getType())) {
                message = "Liked your post";
                holder.followButton.setVisibility(View.GONE);
            } else if ("REPLY".equalsIgnoreCase(model.getType())) {
                message = "Replied to your post: " + (model.getMessage() != null ? model.getMessage() : "");
                holder.followButton.setVisibility(View.GONE);
            } else {
                message = model.getMessage() != null ? model.getMessage() : "Interacted with you";
                holder.followButton.setVisibility(View.GONE);
            }
            holder.message.setText(message);
            
            if (model.getTimestamp() != null) {
                try {
                    holder.timeSpan.setText(Utils.calculateTimeDiff(Long.parseLong(model.getTimestamp())));
                } catch (Exception e) {
                    holder.timeSpan.setText("");
                }
            }

            // Fetch User Info
            if (model.getFromUserId() != null) {
                BaseActivity.mUsersDatabaseReference.child(model.getFromUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserModel user = snapshot.getValue(UserModel.class);
                        if (user != null) {
                            holder.username.setText(user.getUsername());
                            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                                Picasso.get().load(user.getProfileImage()).placeholder(R.drawable.person_outline_24px).into(holder.profileImage);
                            } else {
                                holder.profileImage.setImageResource(R.drawable.person_outline_24px);
                            }
                            
                            // Check follow status
                            holder.itemView.setOnClickListener(v -> {
                                android.content.Intent intent = new android.content.Intent(v.getContext(), com.harsh.shah.threads.clone.activities.OtherUserProfileActivity.class);
                                intent.putExtra("uid", user.getUid());
                                v.getContext().startActivity(intent);
                            });
                            
                            if (BaseActivity.mUser != null && BaseActivity.mUser.getFollowing() != null && BaseActivity.mUser.getFollowing().contains(user.getUid())) {
                                holder.followButton.setText("Following");
                                holder.followButton.setBackgroundResource(R.drawable.button_background_outlined);
                                holder.followButton.setTextColor(getResources().getColor(R.color.textSec));
                            } else {
                                holder.followButton.setText("Follow");
                                holder.followButton.setBackgroundResource(R.drawable.button_background);
                                holder.followButton.setTextColor(getResources().getColor(R.color.textMain));
                            }
                            
                            holder.followButton.setOnClickListener(v -> {
                                if (BaseActivity.mUser == null) return;
                                if (BaseActivity.mUser.getFollowing() != null && BaseActivity.mUser.getFollowing().contains(user.getUid())) {
                                    BaseActivity.mUser.getFollowing().remove(user.getUid());
                                    if (user.getFollowers() != null) user.getFollowers().remove(BaseActivity.mUser.getUid());
                                } else {
                                    if (BaseActivity.mUser.getFollowing() == null) BaseActivity.mUser.setFollowing(new ArrayList<>());
                                    BaseActivity.mUser.getFollowing().add(user.getUid());
                                    if (user.getFollowers() == null) user.setFollowers(new ArrayList<>());
                                    user.getFollowers().add(BaseActivity.mUser.getUid());

                                    // Send notification
                                    String notificationId = BaseActivity.mNotificationsDatabaseReference.push().getKey();
                                    com.harsh.shah.threads.clone.model.NotificationModel notification = new com.harsh.shah.threads.clone.model.NotificationModel(
                                        notificationId,
                                        "FOLLOW",
                                        BaseActivity.mUser.getUid(),
                                        com.harsh.shah.threads.clone.utils.Utils.getNowInMillis() + "",
                                        "",
                                        "started following you"
                                    );
                                    BaseActivity.mNotificationsDatabaseReference.child(user.getUid()).child(notificationId).setValue(notification);
                                }
                                BaseActivity.mUsersDatabaseReference.child(BaseActivity.mUser.getUsername()).setValue(BaseActivity.mUser);
                                BaseActivity.mUsersDatabaseReference.child(user.getUsername()).setValue(user);
                                notifyItemChanged(holder.getAdapterPosition());
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        }

        @Override
        public int getItemCount() {
            return filteredNotifications.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView profileImage;
            TextView username, timeSpan, message, followButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profile_image);
                username = itemView.findViewById(R.id.username);
                timeSpan = itemView.findViewById(R.id.time_span);
                message = itemView.findViewById(R.id.textView12);
                followButton = itemView.findViewById(R.id.follow_button);
            }
        }
    }
}
