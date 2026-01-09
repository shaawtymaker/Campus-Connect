package com.harsh.shah.threads.clone.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.activities.NewThreadActivity;
import com.harsh.shah.threads.clone.activities.ThreadViewActivity;
import com.harsh.shah.threads.clone.model.CommentsModel;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.utils.TextFormatter;
import com.harsh.shah.threads.clone.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import io.getstream.photoview.dialog.PhotoViewDialog;

public class HomeFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "HomeFragment";
    private static final int PAGE_SIZE = 20; // Load 20 threads at a time
    
    private static HomeFragment instance;
    RecyclerView recyclerView;
    private String mParam1;
    private String mParam2;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private ChildEventListener threadsListener; // Track the listener to avoid duplicates
    private String lastLoadedKey = null;

    ArrayList<ThreadModel> data = new ArrayList<>();
    private final Adapter dataAdapter = new Adapter(data);

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    public static Fragment getInstance() {
        if (instance == null)
            instance = new HomeFragment();
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(dataAdapter);
        
        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if (!isLoading && hasMoreData) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                            // Load more when 3 items from bottom
                            loadMoreThreads();
                        }
                    }
                }
            }
        });
        
        refreshList();

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reset pagination state
            lastLoadedKey = null;
            hasMoreData = true;
            isLoading = false;
            refreshList();
            // Stop refreshing after a short delay to allow data to load
            swipeRefreshLayout.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 500);
        });
    }

    private void refreshList() {
        data.clear();
        dataAdapter.clearData();

        // Remove old listener to prevent duplicates
        if (threadsListener != null) {
            BaseActivity.mThreadsDatabaseReference.removeEventListener(threadsListener);
        }
        
        // Create and store new listener
        threadsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ThreadModel threadModel = snapshot.getValue(ThreadModel.class);
                if (threadModel != null) {
                    threadModel.setID(snapshot.getKey());
                    dataAdapter.addData(threadModel);
                    // Track the key of the first thread for pagination
                    if (lastLoadedKey == null || snapshot.getKey().compareTo(lastLoadedKey) < 0) {
                        lastLoadedKey = snapshot.getKey();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ThreadModel model = snapshot.getValue(ThreadModel.class);
                if (model != null) {
                    model.setID(snapshot.getKey());
                    for (int i = 0; i < data.size(); i++) {
                        if (data.get(i).getID().equals(model.getID())) {
                            dataAdapter.updateData(i, model);
                            break;
                        }
                    }
                }
                Log.i(TAG, "onChildChanged: " + snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                ThreadModel model = snapshot.getValue(ThreadModel.class);
                if (model != null)
                    dataAdapter.removeData(model);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        
        // Add the listener
        BaseActivity.mThreadsDatabaseReference.addChildEventListener(threadsListener);
    }
    
    // Load more threads for pagination
    private void loadMoreThreads() {
        if (isLoading || !hasMoreData) return;
        
        // Don't try to paginate if we have no data yet
        if (data.isEmpty() || lastLoadedKey == null || lastLoadedKey.isEmpty()) {
            Log.d(TAG, "Skipping pagination - no data loaded yet");
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "Loading more threads...");
        
        // Load next page of threads
        com.google.firebase.database.Query query = BaseActivity.mThreadsDatabaseReference
                .orderByKey()
                .limitToLast(PAGE_SIZE + 1);
        
        // Only add endBefore if we have a valid lastLoadedKey
        if (lastLoadedKey != null && !lastLoadedKey.isEmpty()) {
            query = query.endBefore(lastLoadedKey);
        }
        
        query.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            if (count >= PAGE_SIZE) {
                                // We have more data - keep hasMoreData true
                                hasMoreData = true;
                                break;
                            }
                            
                            ThreadModel threadModel = childSnapshot.getValue(ThreadModel.class);
                            if (threadModel != null && !containsThread(threadModel.getID())) {
                                dataAdapter.addData(threadModel);
                                lastLoadedKey = childSnapshot.getKey();
                                count++;
                            }
                        }
                        
                        if (count == 0) {
                            hasMoreData = false;
                        }
                        
                        isLoading = false;
                        Log.d(TAG, "Loaded " + count + " more threads. hasMore: " + hasMoreData);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isLoading = false;
                        Log.e(TAG, "Error loading more threads: " + error.getMessage());
                    }
                });
    }
    
    // Check if thread already exists in data
    private boolean containsThread(String threadId) {
        for (ThreadModel thread : data) {
            if (thread.getID().equals(threadId)) {
                return true;
            }
        }
        return false;
    }

    private void setHeaderPos(TextView view, boolean isActivated) {
        if (isActivated) {
            TextViewCompat.setTextAppearance(view, R.style.Base_Widget_AppCompat_TextView_ButtonFilled);
            view.setBackgroundResource(R.drawable.button_background_filled);
        } else {
            TextViewCompat.setTextAppearance(view, R.style.Base_Widget_AppCompat_TextView_ButtonOutlined);
            view.setBackgroundResource(R.drawable.button_background_outlined);
            view.setEnabled(false);
        }
    }

    public static class PostImagesListAdapter extends RecyclerView.Adapter<PostImagesListAdapter.ViewHolder> {

        private boolean shouldHaveLeftPadding = true;
        private List<String> data = new ArrayList<>();

        public PostImagesListAdapter() {
        }

        public PostImagesListAdapter(List<String> data) {
            this.data = data;
        }

        public PostImagesListAdapter(List<String> data, boolean leftPadding) {
            this.data = data;
            this.shouldHaveLeftPadding = leftPadding;
        }

        public PostImagesListAdapter(boolean leftPadding) {
            this.shouldHaveLeftPadding = leftPadding;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_list_item_image_item, null);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(view, getItemCount());
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            int dp = ((int) (holder.itemView.getContext().getResources().getDisplayMetrics().density));
            params.setMargins((position == 0 && shouldHaveLeftPadding) ? dp * 60 : dp * 4, 0, dp * 4, dp * 4);
            holder.itemView.setLayoutParams(params);
            ImageView imageView = holder.itemView.findViewById(R.id.imageView);
            if (!data.get(position).isEmpty()) {
                if (data.get(position).contains("gif"))
                    Glide.with(holder.itemView.getContext()).asGif().load(data.get(position)).into(imageView);
                else
                    Glide.with(holder.itemView.getContext()).load(data.get(position)).into(imageView);
            }

            imageView.setOnClickListener(view -> {
                new PhotoViewDialog.Builder(holder.itemView.getContext(), data, (imageView1, url) -> Glide.with(holder.itemView.getContext()).load(url).into(imageView1)).withTransitionFrom(imageView).withStartPosition(position).build().show(true);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView, int itemCount) {
                super(itemView);
            }
        }
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private ArrayList<ThreadModel> data = new ArrayList<>();

        public Adapter(ArrayList<ThreadModel> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = viewType == 1 ? LayoutInflater.from(getContext()).inflate(R.layout.main_head_layout, null) : LayoutInflater.from(getContext()).inflate(R.layout.home_list_item, null);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (getItemViewType(position) == 1) {
                holder.itemView.setOnClickListener(view -> startActivity(new Intent(getContext(), NewThreadActivity.class)));
                if (BaseActivity.mUser != null && BaseActivity.mUser.getUsername() != null)
                    ((TextView) holder.itemView.findViewById(R.id.username)).setText(BaseActivity.mUser.getUsername());
                return;
            }

            if (position == 0) return;
            final int newPosition = position - 1;

            ThreadModel model = data.get(newPosition);
            if (model == null) return;

            TextView usernameView = holder.itemView.findViewById(R.id.username);
            if (model.getUsername() != null) {
                usernameView.setText(model.getUsername());
            } else {
                usernameView.setText("Unknown");
            }

            final ShapeableImageView profileImage = holder.itemView.findViewById(R.id.profileImage);
            if (model.profileImage() != null && !model.profileImage().isEmpty()) {
                Picasso.get().load(model.profileImage()).placeholder(R.drawable.person_outline_24px).into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.person_outline_24px);
            }

            ((TextView) holder.itemView.findViewById(R.id.time)).setText(Utils.calculateTimeDiff(Long.parseLong(model.getTime())));
            
            // Format text with clickable hashtags and mentions
            TextView titleView = holder.itemView.findViewById(R.id.title);
            TextFormatter.applyFormattedText(titleView, model.getText() != null ? model.getText() : "", getContext());

            int likesCount = model.getLikes() != null ? model.getLikes().size() : 0;
            int commentsCount = model.getComments() != null ? model.getComments().size() : 0;
            int repostsCount = model.getReposts() != null ? model.getReposts().size() : 0;

            ((TextView) holder.itemView.findViewById(R.id.likes)).setText(String.valueOf(likesCount));
            ((TextView) holder.itemView.findViewById(R.id.comments)).setText(String.valueOf(commentsCount));
            ((TextView) holder.itemView.findViewById(R.id.reposts)).setText(String.valueOf(repostsCount));

            ((RecyclerView) holder.itemView.findViewById(R.id.imagesListRecyclerView)).setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

            if (model.getImages() != null && !model.getImages().isEmpty())
                ((RecyclerView) holder.itemView.findViewById(R.id.imagesListRecyclerView)).setAdapter(new PostImagesListAdapter(model.getImages()));

            if (model.isIsPoll() && model.getPollOptions() != null) {
                holder.itemView.findViewById(R.id.poll_layout).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.imagesListRecyclerView).setVisibility(View.GONE);

                if (model.getPollOptions().getOption1() != null)
                    ((TextView) holder.itemView.findViewById(R.id.poll_option_1)).setText(model.getPollOptions().getOption1().getText());

                if (model.getPollOptions().getOption2() != null)
                    ((TextView) holder.itemView.findViewById(R.id.poll_option_2)).setText(model.getPollOptions().getOption2().getText());

                if (model.getPollOptions().getOption3() != null && model.getPollOptions().getOption3().getVisibility()) {
                    ((TextView) holder.itemView.findViewById(R.id.poll_option_3)).setVisibility(View.VISIBLE);
                    ((TextView) holder.itemView.findViewById(R.id.poll_option_3)).setText(model.getPollOptions().getOption3().getText());
                }
                if (model.getPollOptions().getOption4() != null && model.getPollOptions().getOption4().getVisibility()) {
                    ((TextView) holder.itemView.findViewById(R.id.poll_option_4)).setVisibility(View.VISIBLE);
                    ((TextView) holder.itemView.findViewById(R.id.poll_option_4)).setText(model.getPollOptions().getOption4().getText());
                }
            } else {
                holder.itemView.findViewById(R.id.poll_layout).setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(view -> startActivity(new Intent(getContext(), ThreadViewActivity.class).putExtra("thread", model.getID())));

            // Poll voting logic - Set up click listeners for each poll option
            if (model.isIsPoll() && model.getPollOptions() != null && BaseActivity.mUser != null) {
                String userId = BaseActivity.mUser.getUid();
                
                // Check if user has already voted
                boolean hasVoted = false;
                if (model.getPollOptions().getOption1() != null && model.getPollOptions().getOption1().getVotes().contains(userId)) hasVoted = true;
                if (model.getPollOptions().getOption2() != null && model.getPollOptions().getOption2().getVotes().contains(userId)) hasVoted = true;
                if (model.getPollOptions().getOption3() != null && model.getPollOptions().getOption3().getVotes().contains(userId)) hasVoted = true;
                if (model.getPollOptions().getOption4() != null && model.getPollOptions().getOption4().getVotes().contains(userId)) hasVoted = true;
                
                // Poll Option 1 Click
                TextView pollOption1 = holder.itemView.findViewById(R.id.poll_option_1);
                if (model.getPollOptions().getOption1() != null && model.getPollOptions().getOption1().getVisibility()) {
                    boolean isVoted = model.getPollOptions().getOption1().getVotes().contains(userId);
                    setHeaderPos(pollOption1, isVoted);
                    
                    pollOption1.setOnClickListener(v -> {
                        if (!isVoted) {
                            votePoll(model, 1, newPosition);
                        }
                    });
                }
                
                // Poll Option 2 Click
                TextView pollOption2 = holder.itemView.findViewById(R.id.poll_option_2);
                if (model.getPollOptions().getOption2() != null && model.getPollOptions().getOption2().getVisibility()) {
                    boolean isVoted = model.getPollOptions().getOption2().getVotes().contains(userId);
                    setHeaderPos(pollOption2, isVoted);
                    
                    pollOption2.setOnClickListener(v -> {
                        if (!isVoted) {
                            votePoll(model, 2, newPosition);
                        }
                    });
                }
                
                // Poll Option 3 Click
                TextView pollOption3 = holder.itemView.findViewById(R.id.poll_option_3);
                if (model.getPollOptions().getOption3() != null && model.getPollOptions().getOption3().getVisibility()) {
                    boolean isVoted = model.getPollOptions().getOption3().getVotes().contains(userId);
                    setHeaderPos(pollOption3, isVoted);
                    
                    pollOption3.setOnClickListener(v -> {
                        if (!isVoted) {
                            votePoll(model, 3, newPosition);
                        }
                    });
                }
                
                // Poll Option 4 Click
                TextView pollOption4 = holder.itemView.findViewById(R.id.poll_option_4);
                if (model.getPollOptions().getOption4() != null && model.getPollOptions().getOption4().getVisibility()) {
                    boolean isVoted = model.getPollOptions().getOption4().getVotes().contains(userId);
                    setHeaderPos(pollOption4, isVoted);
                    
                    pollOption4.setOnClickListener(v -> {
                        if (!isVoted) {
                            votePoll(model, 4, newPosition);
                        }
                    });
                }
            }
            
            // Bookmark button click listener
            ImageView bookmarkIcon = holder.itemView.findViewById(R.id.bookmarkImage);
            if (BaseActivity.mUser != null && model.getID() != null) {
                // Set initial bookmark state
                boolean isSaved = BaseActivity.mUser.getSavedThreads() != null && 
                                  BaseActivity.mUser.getSavedThreads().contains(model.getID());
                updateBookmarkIcon(bookmarkIcon, isSaved);
                
                bookmarkIcon.setOnClickListener(v -> {
                    toggleSaveThread(model, bookmarkIcon);
                });
            }

            holder.itemView.findViewById(R.id.likeThreadLayout).setOnClickListener(view -> {
                if (model.getLikes() == null || BaseActivity.mUser == null) return;

                if (model.getLikes().contains(BaseActivity.mUser.getUid())) {
                    model.getLikes().remove(BaseActivity.mUser.getUid());
                } else {
                    model.getLikes().add(BaseActivity.mUser.getUid());
                }
                setLikeStatus(holder, newPosition);
                BaseActivity.mThreadsDatabaseReference.child(model.getID()).setValue(model);
                notifyDataSetChanged();
            });
            setLikeStatus(holder, newPosition);
            
            // Repost button click listener
            holder.itemView.findViewById(R.id.linearLayout3).setOnClickListener(view -> {
                showRepostDialog(model);
            });
            
            // Share button click listener
            holder.itemView.findViewById(R.id.linearLayout4).setOnClickListener(view -> {
                shareThread(model);
            });
            
            // Delete button click listener - only show for user's own threads
            ImageView moreOptionsBtn = holder.itemView.findViewById(R.id.moreOptionsImage);
            if (model.getUserId() != null && BaseActivity.mUser != null && 
                model.getUserId().equals(BaseActivity.mUser.getUid())) {
                moreOptionsBtn.setVisibility(View.VISIBLE);
                moreOptionsBtn.setOnClickListener(view -> showDeleteDialog(model));
            } else {
                moreOptionsBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 1 : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
        
        // Handle poll voting
        private void votePoll(ThreadModel thread, int optionNumber, int position) {
            if (thread == null || thread.getPollOptions() == null || BaseActivity.mUser == null) {
                return;
            }
            
            String userId = BaseActivity.mUser.getUid();
            
            // Check if user already voted on any option (prevent multiple votes)
            boolean alreadyVoted = false;
            if (thread.getPollOptions().getOption1() != null && thread.getPollOptions().getOption1().getVotes().contains(userId)) alreadyVoted = true;
            if (thread.getPollOptions().getOption2() != null && thread.getPollOptions().getOption2().getVotes().contains(userId)) alreadyVoted = true;
            if (thread.getPollOptions().getOption3() != null && thread.getPollOptions().getOption3().getVotes().contains(userId)) alreadyVoted = true;
            if (thread.getPollOptions().getOption4() != null && thread.getPollOptions().getOption4().getVotes().contains(userId)) alreadyVoted = true;
            
            if (alreadyVoted) {
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "You've already voted on this poll", android.widget.Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            // Add vote to selected option
            switch (optionNumber) {
                case 1:
                    if (thread.getPollOptions().getOption1() != null) {
                        thread.getPollOptions().getOption1().getVotes().add(userId);
                    }
                    break;
                case 2:
                    if (thread.getPollOptions().getOption2() != null) {
                        thread.getPollOptions().getOption2().getVotes().add(userId);
                    }
                    break;
                case 3:
                    if (thread.getPollOptions().getOption3() != null) {
                        thread.getPollOptions().getOption3().getVotes().add(userId);
                    }
                    break;
                case 4:
                    if (thread.getPollOptions().getOption4() != null) {
                        thread.getPollOptions().getOption4().getVotes().add(userId);
                    }
                    break;
            }
            
            // Update in Firebase
            BaseActivity.mThreadsDatabaseReference.child(thread.getID()).setValue(thread)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Vote recorded!", android.widget.Toast.LENGTH_SHORT).show();
                    }
                    // Update UI
                    notifyItemChanged(position + 1);
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Failed to record vote", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        }

        private void updateVote(String id, ThreadModel dataModel) {
            BaseActivity.mThreadsDatabaseReference.child(id).setValue(dataModel);
        }

        public void updateData(int position, ThreadModel data) {
            this.data.set(position, data);
            notifyDataSetChanged();
            notifyItemChanged(position);
        }

        public void addData(ThreadModel data) {
            this.data.add(0, data);
            notifyDataSetChanged();
            notifyItemInserted(this.data.size());
        }

        public void removeData(int position) {
            this.data.remove(position);
            notifyDataSetChanged();
            notifyItemRemoved(position + 1);
        }

        public void removeData(ThreadModel model) {
            int i = this.data.indexOf(model);
            this.data.remove(model);
            notifyDataSetChanged();
            notifyItemRemoved(i);
        }

        public void clearData() {
            this.data.clear();
            notifyDataSetChanged();
        }

        private void setLikeStatus(ViewHolder holder, int newPosition) {
            ThreadModel model = data.get(newPosition);
            if (model == null || model.getLikes() == null || BaseActivity.mUser == null) return;

            if (model.getLikes().contains(BaseActivity.mUser.getUid())) {
                ((ImageView) holder.itemView.findViewById(R.id.likeImage)).setImageResource(R.drawable.favorite_24px);
            } else {
                ((ImageView) holder.itemView.findViewById(R.id.likeImage)).setImageResource(R.drawable.favorite_outline_24px);
                ((ImageView) holder.itemView.findViewById(R.id.likeImage)).setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.red));
            }
        }
        
        // Share thread functionality
        private void shareThread(ThreadModel thread) {
            if (getContext() == null || thread == null) return;
            
            String shareText = buildShareText(thread);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this thread");
            
            try {
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            } catch (Exception e) {
                Log.e(TAG, "Error sharing thread", e);
            }
        }
        
        private String buildShareText(ThreadModel thread) {
            StringBuilder text = new StringBuilder();
            
            if (thread.getText() != null && !thread.getText().isEmpty()) {
                text.append(thread.getText());
            }
            
            text.append("\n\n");
            text.append("Posted by @").append(thread.getUsername());
            
            // Add app link (you can customize this with your actual domain)
            text.append("\n\nView on Campus Connect");
            // text.append("\nhttps://your-domain.com/thread/").append(thread.getID());
            
            return text.toString();
        }
        
        // Show delete confirmation dialog
        private void showDeleteDialog(ThreadModel thread) {
            if (getContext() == null || thread == null) return;
            
            new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Thread")
                .setMessage("Are you sure you want to delete this thread? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteThread(thread);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
        
        // Delete thread from Firebase
        private void deleteThread(ThreadModel thread) {
            if (thread == null || thread.getID() == null) return;
            
            BaseActivity.mThreadsDatabaseReference.child(thread.getID()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Thread deleted", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Failed to delete thread", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        }
        
        // Show repost dialog
        private void showRepostDialog(ThreadModel thread) {
            if (getContext() == null || thread == null) return;
            
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(R.layout.dialog_repost)
                .create();
            
            dialog.show();
            
            View dialogView = dialog.findViewById(android.R.id.content);
            if (dialogView != null) {
                // Simple repost
                dialogView.findViewById(R.id.simpleRepostLayout).setOnClickListener(v -> {
                    handleSimpleRepost(thread);
                    dialog.dismiss();
                });
                
                // Quote repost (coming soon)
                dialogView.findViewById(R.id.quoteRepostLayout).setOnClickListener(v -> {
                    android.widget.Toast.makeText(getContext(), "Quote repost coming soon!", android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
                
                // Cancel button
                dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
            }
        }
        
        // Handle simple repost
        private void handleSimpleRepost(ThreadModel thread) {
            if (BaseActivity.mUser == null || thread == null || thread.getID() == null || thread.getID().isEmpty()) {
                if (getContext() != null) {
                    String error = BaseActivity.mUser == null ? "User not logged in" : "Error: Thread ID is missing";
                    android.widget.Toast.makeText(getContext(), error, android.widget.Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            String userId = BaseActivity.mUser.getUid();
            
            if (thread.getReposts() == null) {
                thread.setReposts(new ArrayList<>());
            }
            
            if (thread.getReposts().contains(userId)) {
                // Un-repost
                thread.getReposts().remove(userId);
                android.widget.Toast.makeText(getContext(), "Removed repost", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // Repost
                thread.getReposts().add(userId);
                android.widget.Toast.makeText(getContext(), "Reposted!", android.widget.Toast.LENGTH_SHORT).show();
            }
            
            // Update in Firebase
            BaseActivity.mThreadsDatabaseReference.child(thread.getID()).setValue(thread);
            notifyDataSetChanged();
        }
        
        // Toggle save/unsave thread
        private void toggleSaveThread(ThreadModel thread, ImageView bookmarkIcon) {
            if (BaseActivity.mUser == null || thread == null || thread.getID() == null) {
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Error saving thread", android.widget.Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            if (BaseActivity.mUser.getSavedThreads() == null) {
                BaseActivity.mUser.setSavedThreads(new ArrayList<>());
            }
            
            boolean isSaved = BaseActivity.mUser.getSavedThreads().contains(thread.getID());
            
            if (isSaved) {
                // Unsave
                BaseActivity.mUser.getSavedThreads().remove(thread.getID());
                updateBookmarkIcon(bookmarkIcon, false);
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Removed from saved", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                // Save
                BaseActivity.mUser.getSavedThreads().add(thread.getID());
                updateBookmarkIcon(bookmarkIcon, true);
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Thread saved!", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            
            // Update in Firebase
            BaseActivity.mUsersDatabaseReference.child(BaseActivity.mUser.getUid())
                .child("savedThreads")
                .setValue(BaseActivity.mUser.getSavedThreads());
        }
        
        // Update bookmark icon based on save state
        private void updateBookmarkIcon(ImageView icon, boolean isSaved) {
            if (isSaved) {
                icon.setImageResource(R.drawable.favorite_24px); // Use filled icon as bookmark
                icon.setColorFilter(getResources().getColor(R.color.blue));
            } else {
                icon.setImageResource(R.drawable.add_24px); // Use outline icon
                icon.setColorFilter(getResources().getColor(R.color.textSec));
            }
        }
    }
}