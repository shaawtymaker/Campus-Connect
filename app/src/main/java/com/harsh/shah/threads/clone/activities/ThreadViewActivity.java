package com.harsh.shah.threads.clone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityThreadViewBinding;
import com.harsh.shah.threads.clone.fragments.HomeFragment;
import com.harsh.shah.threads.clone.model.CommentsModel;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.utils.TextFormatter;
import com.harsh.shah.threads.clone.utils.Utils;

import java.util.ArrayList;

public class ThreadViewActivity extends BaseActivity {

    ActivityThreadViewBinding binding;
    private ThreadModel currentThreadModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThreadViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.postRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.postRecyclerView.setAdapter(new HomeFragment.PostImagesListAdapter(false));

        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (getIntent().getExtras() == null || (getIntent().getExtras().getString("thread") == null))
            finish();

        // Load current user data
        fetchCurrentUser(user -> {
            if (user != null) {
                mUser = user;
            }
        });

        mThreadsDatabaseReference.child(getIntent().getExtras().getString("thread")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ThreadModel threadModel = snapshot.getValue(ThreadModel.class);
                if (threadModel == null) {
                    // Thread was deleted, close activity
                    finish();
                    return; // Don't continue processing
                }
                // Set the thread ID from the Firebase key
                threadModel.setID(snapshot.getKey());
                android.util.Log.d("ThreadViewActivity", "Thread ID set to: " + threadModel.getID() + " from key: " + snapshot.getKey());
                currentThreadModel = threadModel;
                setUpThreadView(threadModel);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.addNewCommentLayout.setOnClickListener(view -> {
            if (mUser == null) {
                Toast.makeText(this, "User data not loaded", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!mUser.isPublicAccount()) {
                showNeedPublicProfileDialog();
                return;
            }
            addNewComment();
        });

        // Post comment button handler - must use findViewById since visibility=gone
        binding.getRoot().findViewById(R.id.post_comment_button).setOnClickListener(v -> {
            android.util.Log.d("ThreadViewActivity", "Post button clicked!");
            postComment();
        });

        // Repost button listener
        binding.linearLayout3.setOnClickListener(v -> {
            if (currentThreadModel != null) {
                showRepostDialog(currentThreadModel);
            }
        });
    }

    private void addNewComment() {
        android.util.Log.d("ThreadViewActivity", "addNewComment called!");
        if (currentThreadModel == null) {
            Toast.makeText(this, "Thread not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        // Show the comment input box - use findViewById since visibility=gone
        LinearLayout commentLayout = binding.getRoot().findViewById(R.id.add_new_comment_layout);
        EditText commentEditText = binding.getRoot().findViewById(R.id.comment_edittext);
        
        android.util.Log.d("ThreadViewActivity", "commentLayout found: " + (commentLayout != null));
        
        if (commentLayout.getVisibility() == View.VISIBLE) {
            // Hide if already visible
            commentLayout.setVisibility(View.GONE);
            commentEditText.setText("");
        } else {
            // Show and focus
            commentLayout.setVisibility(View.VISIBLE);
            commentEditText.requestFocus();
            // Show keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(commentEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void postComment() {
        EditText commentEditText = findViewById(R.id.comment_edittext);
        String commentText = commentEditText.getText().toString().trim();
        
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentThreadModel == null) {
            Toast.makeText(this, "Error: Thread not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create new comment
        CommentsModel comment = new CommentsModel();
        comment.setText(commentText);
        comment.setUid(mUser.getUid());
        comment.setUsername(mUser.getUsername());
        comment.setProfileImage(mUser.getProfileImage());
        comment.setTime(String.valueOf(Utils.getNowInMillis()));
        
        // Get thread ID from the intent
        String threadId = getIntent().getExtras().getString("thread");
        
        // Generate Firebase ID for the comment FIRST
        String commentId = mThreadsDatabaseReference.child(threadId).child("comments").push().getKey();
        comment.setId(commentId);
        
        // Add comment to local model for UI update
        if (currentThreadModel.getComments() == null) {
            currentThreadModel.setComments(new HashMap<>());
        }
        currentThreadModel.getComments().put(commentId, comment);
        
        // Save ONLY the comment to Firebase (not entire ThreadModel - HashMap issue)
        if (threadId != null && !threadId.isEmpty()) {
            mThreadsDatabaseReference.child(threadId).child("comments").child(commentId).setValue(comment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show();
                    commentEditText.setText("");
                    LinearLayout commentLayout = findViewById(R.id.add_new_comment_layout);
                    commentLayout.setVisibility(View.GONE);
                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(commentEditText.getWindowToken(), 0);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            Toast.makeText(this, "Error: Thread ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNeedPublicProfileDialog(){
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ThreadViewActivity.this);
        final View bottomSheetView = LayoutInflater.from(ThreadViewActivity.this).inflate(R.layout.public_account_needed_dialog_layout, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        final LinearLayout privateProfileButton, publicProfileButton;
        final TextView cancel_button;
        cancel_button = bottomSheetView.findViewById(R.id.cancel_button);
        privateProfileButton = bottomSheetView.findViewById(R.id.privateProfileButton);
        publicProfileButton = bottomSheetView.findViewById(R.id.publicProfileBtn);

        final boolean publicAccountStatus = mUser.isPublicAccount();
        if(publicAccountStatus){
            publicProfileButton.setBackgroundResource(R.drawable.button_background_outlined_filled);
            privateProfileButton.setBackgroundResource(R.drawable.button_background_outlined);
        }else{
            publicProfileButton.setBackgroundResource(R.drawable.button_background_outlined);
            privateProfileButton.setBackgroundResource(R.drawable.button_background_outlined_filled);
        }

        privateProfileButton.setOnClickListener(view1 -> {
            publicProfileButton.setBackgroundResource(R.drawable.button_background_outlined);
            privateProfileButton.setBackgroundResource(R.drawable.button_background_outlined_filled);
            setHeaderPos(cancel_button, publicAccountStatus);
            cancel_button.setText(publicAccountStatus?"Done":"Cancel");
            cancel_button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        });
        publicProfileButton.setOnClickListener(view1 -> {
            publicProfileButton.setBackgroundResource(R.drawable.button_background_outlined_filled);
            privateProfileButton.setBackgroundResource(R.drawable.button_background_outlined);
            setHeaderPos(cancel_button, !publicAccountStatus);
            cancel_button.setText(!publicAccountStatus?"Done":"Cancel");
            cancel_button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        });

        cancel_button.setOnClickListener(view1 -> {
            bottomSheetDialog.dismiss();
            if(cancel_button.getText().toString().equals("Cancel"))
                return;
            showProgressDialog();
            mUser.setPublicAccount(!publicAccountStatus);
            mUsersDatabaseReference.child(mUser.getUsername()).setValue(mUser).addOnCompleteListener(task -> hideProgressDialog());
        });

        bottomSheetDialog.create();
        bottomSheetDialog.show();
    }

    private void setUpThreadView(ThreadModel threadModel){
        // Format text with clickable hashtags and mentions
        TextFormatter.applyFormattedText(binding.textPara, threadModel.getText(), this);
        binding.username.setText(threadModel.getUsername());
        
        // Handle time parsing safely
        if (threadModel.getTime() != null && !threadModel.getTime().isEmpty()) {
            try {
                binding.time.setText(Utils.calculateTimeDiff(Long.parseLong(threadModel.getTime())));
            } catch (NumberFormatException e) {
                binding.time.setText("");
            }
        } else {
            binding.time.setText("");
        }

        binding.likes.setText(String.valueOf(threadModel.getLikes() == null?0:threadModel.getLikes().size()));
        binding.comments.setText(String.valueOf(threadModel.getComments() == null?0:threadModel.getComments().size()));
        binding.reposts.setText(String.valueOf(threadModel.getReposts() == null?0:threadModel.getReposts().size()));

        binding.postRecyclerView.setAdapter(new HomeFragment.PostImagesListAdapter(threadModel.getImages(), false));
        
        // Link Preview Logic
        android.view.View linkPreviewCard = binding.getRoot().findViewById(R.id.link_preview_card);
        
        if ((threadModel.getImages() == null || threadModel.getImages().isEmpty()) && 
            threadModel.getLinkUrl() != null && !threadModel.getLinkUrl().isEmpty()) {
            
            linkPreviewCard.setVisibility(View.VISIBLE);
            binding.postRecyclerView.setVisibility(View.GONE); 
            
            TextView linkTitle = binding.getRoot().findViewById(R.id.link_preview_title);
            TextView linkDesc = binding.getRoot().findViewById(R.id.link_preview_description);
            TextView linkUrl = binding.getRoot().findViewById(R.id.link_preview_url);
            ImageView linkImage = binding.getRoot().findViewById(R.id.link_preview_image);
            
            linkTitle.setText(threadModel.getLinkTitle() != null && !threadModel.getLinkTitle().isEmpty() ? threadModel.getLinkTitle() : threadModel.getLinkUrl());
            linkDesc.setText(threadModel.getLinkDescription() != null ? threadModel.getLinkDescription() : "");
            linkUrl.setText(threadModel.getLinkUrl());
            
            if (threadModel.getLinkImage() != null && !threadModel.getLinkImage().isEmpty()) {
                linkImage.setVisibility(View.VISIBLE);
                com.squareup.picasso.Picasso.get().load(threadModel.getLinkImage()).into(linkImage);
            } else {
                linkImage.setVisibility(View.GONE);
            }
            
            linkPreviewCard.setOnClickListener(v -> {
                 try {
                     Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(threadModel.getLinkUrl()));
                     startActivity(browserIntent);
                 } catch (Exception e) {
                     Log.e("ThreadViewActivity", "Error opening link", e);
                 }
            });
            
        } else {
            linkPreviewCard.setVisibility(View.GONE);
            // Only show image recycler if images exist
            if (threadModel.getImages() != null && !threadModel.getImages().isEmpty()) {
                binding.postRecyclerView.setVisibility(View.VISIBLE);
            } else {
                binding.postRecyclerView.setVisibility(View.GONE);
            }
        }

        // Enable poll display
        if(threadModel.isIsPoll() && threadModel.getPollOptions() != null) {
            binding.pollLayout.setVisibility(View.VISIBLE);
            binding.postRecyclerView.setVisibility(View.GONE);
            linkPreviewCard.setVisibility(View.GONE); // Polls take precedence
            
            // Display poll options and set up voting
            setupPollVoting(threadModel);
        }
        else {
            binding.pollLayout.setVisibility(View.GONE);
            // Visibility of other items handled above
        }

        if(threadModel.getComments()!=null && !threadModel.getComments().isEmpty()){
            binding.commentsRecyclerView.setAdapter(new CommentsImagesListAdapter(threadModel.getCommentsAsList()));
        } else {
            // Set empty adapter to avoid "no adapter" errors
            binding.commentsRecyclerView.setAdapter(new CommentsImagesListAdapter(new ArrayList<>()));
        }

        // Comment icon click listener
        binding.linearLayout.setOnClickListener(view -> {
            android.util.Log.d("ThreadViewActivity", "Comment icon clicked!");
            if (mAuth.getCurrentUser() == null) {
                startActivity(new Intent(ThreadViewActivity.this, AuthActivity.class));
                return;
            }
            if (mUser != null && !mUser.isPublicAccount()) {
                showNeedPublicProfileDialog();
                return;
            }
            addNewComment();
        });

        binding.likesLayout.setOnClickListener(view -> {
            if (BaseActivity.mUser == null) {
                Toast.makeText(this, "User data not loaded", Toast.LENGTH_SHORT).show();
                return;
            }
            if (threadModel.getLikes().contains(BaseActivity.mUser.getUid())) {
                threadModel.getLikes().remove(BaseActivity.mUser.getUid());
            } else {
                threadModel.getLikes().add(BaseActivity.mUser.getUid());
            }
            setLikeStatus(binding.likes, threadModel);
            BaseActivity.mThreadsDatabaseReference.child(threadModel.getID()).setValue(threadModel);
        });
        setLikeStatus(binding.likes, threadModel);
    }

    private void setLikeStatus(TextView likes, ThreadModel threadModel) {
        if (BaseActivity.mUser == null) {
            binding.likeImage.setImageResource(R.drawable.favorite_outline_24px);
            binding.likeImage.setColorFilter(getResources().getColor(R.color.red));
            return;
        }
        if (threadModel.getLikes().contains(BaseActivity.mUser.getUid())) {
            binding.likeImage.setImageResource(R.drawable.favorite_24px);
        } else {
            binding.likeImage.setImageResource(R.drawable.favorite_outline_24px);
            binding.likeImage.setColorFilter(getResources().getColor(R.color.red));
        }
    }
    
    // Setup poll voting functionality
    private void setupPollVoting(ThreadModel thread) {
        if (thread.getPollOptions() == null || mUser == null) return;
        
        String userId = mUser.getUid();
        
        // Display option 1
        if (thread.getPollOptions().getOption1() != null && thread.getPollOptions().getOption1().getVisibility()) {
            binding.pollOption1.setText(thread.getPollOptions().getOption1().getText());
            boolean isVoted = thread.getPollOptions().getOption1().getVotes().contains(userId);
            setHeaderPos(binding.pollOption1, isVoted);
            
            binding.pollOption1.setOnClickListener(v -> {
                if (!isVoted) {
                    votePoll(thread, 1);
                } else {
                    Toast.makeText(this, "You've already voted", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Display option 2
        if (thread.getPollOptions().getOption2() != null && thread.getPollOptions().getOption2().getVisibility()) {
            binding.pollOption2.setText(thread.getPollOptions().getOption2().getText());
            boolean isVoted = thread.getPollOptions().getOption2().getVotes().contains(userId);
            setHeaderPos(binding.pollOption2, isVoted);
            
            binding.pollOption2.setOnClickListener(v -> {
                if (!isVoted) {
                    votePoll(thread, 2);
                } else {
                    Toast.makeText(this, "You've already voted", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Display option 3
        if (thread.getPollOptions().getOption3() != null && thread.getPollOptions().getOption3().getVisibility()) {
            binding.pollOption3.setVisibility(View.VISIBLE);
            binding.pollOption3.setText(thread.getPollOptions().getOption3().getText());
            boolean isVoted = thread.getPollOptions().getOption3().getVotes().contains(userId);
            setHeaderPos(binding.pollOption3, isVoted);
            
            binding.pollOption3.setOnClickListener(v -> {
                if (!isVoted) {
                    votePoll(thread, 3);
                } else {
                    Toast.makeText(this, "You've already voted", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Display option 4
        if (thread.getPollOptions().getOption4() != null && thread.getPollOptions().getOption4().getVisibility()) {
            binding.pollOption4.setVisibility(View.VISIBLE);
            binding.pollOption4.setText(thread.getPollOptions().getOption4().getText());
            boolean isVoted = thread.getPollOptions().getOption4().getVotes().contains(userId);
            setHeaderPos(binding.pollOption4, isVoted);
            
            binding.pollOption4.setOnClickListener(v -> {
                if (!isVoted) {
                    votePoll(thread, 4);
                } else {
                    Toast.makeText(this, "You've already voted", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    // Handle poll voting
    private void votePoll(ThreadModel thread, int optionNumber) {
        if (thread == null || thread.getPollOptions() == null || mUser == null) {
            return;
        }
        
        String userId = mUser.getUid();
        
        // Check if user already voted on any option
        boolean alreadyVoted = false;
        if (thread.getPollOptions().getOption1() != null && thread.getPollOptions().getOption1().getVotes().contains(userId)) alreadyVoted = true;
        if (thread.getPollOptions().getOption2() != null && thread.getPollOptions().getOption2().getVotes().contains(userId)) alreadyVoted = true;
        if (thread.getPollOptions().getOption3() != null && thread.getPollOptions().getOption3().getVotes().contains(userId)) alreadyVoted = true;
        if (thread.getPollOptions().getOption4() != null && thread.getPollOptions().getOption4().getVotes().contains(userId)) alreadyVoted = true;
        
        if (alreadyVoted) {
            Toast.makeText(this, "You've already voted on this poll", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Vote recorded!", Toast.LENGTH_SHORT).show();
                // UI will auto-update via Firebase listener
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to record vote", Toast.LENGTH_SHORT).show();
            });
    }

    private void setHeaderPos(TextView view, boolean isActivated) {
        if (isActivated) {
            view.setBackgroundResource(R.drawable.button_background_outlined_filled);
            view.setTextColor(getResources().getColor(R.color.textMain));
        } else {
            view.setBackgroundResource(R.drawable.button_background_outlined);
            view.setTextColor(getResources().getColor(R.color.textSec));
        }
    }

    // Show repost dialog
    private void showRepostDialog(ThreadModel thread) {
        if (thread == null) return;
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
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
                Toast.makeText(this, "Quote repost coming soon!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            
            // Cancel button
            dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        }
    }
    
    // Handle simple repost
    private void handleSimpleRepost(ThreadModel thread) {
        if (BaseActivity.mUser == null || thread == null || thread.getID() == null || thread.getID().isEmpty()) {
            String error = BaseActivity.mUser == null ? "User not logged in" : "Error: Thread ID is missing";
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = BaseActivity.mUser.getUid();
        
        if (thread.getReposts() == null) {
            thread.setReposts(new ArrayList<>());
        }
        
        if (thread.getReposts().contains(userId)) {
            // Un-repost
            thread.getReposts().remove(userId);
            Toast.makeText(this, "Removed repost", Toast.LENGTH_SHORT).show();
        } else {
            // Repost
            thread.getReposts().add(userId);
            Toast.makeText(this, "Reposted!", Toast.LENGTH_SHORT).show();
        }
        
        // Update in Firebase
        BaseActivity.mThreadsDatabaseReference.child(thread.getID()).setValue(thread);
    }
    
    // Handle comment like/unlike
    private void handleCommentLike(CommentsModel comment, int position) {
        if (mUser == null || comment == null) {
            Toast.makeText(this, "Error: Unable to like comment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mUser.getUid();
        
        if (comment.getLikes() == null) {
            comment.setLikes(new ArrayList<>());
        }
        
        if (comment.getLikes().contains(userId)) {
            // Unlike
            comment.getLikes().remove(userId);
        } else {
            // Like
            comment.getLikes().add(userId);
        }
        
        // Update in Firebase
        if (currentThreadModel != null && currentThreadModel.getID() != null) {
            mThreadsDatabaseReference.child(currentThreadModel.getID())
                .child("comments")
                .child(comment.getId())
                .setValue(comment);
        }
        
        // Update UI
        binding.commentsRecyclerView.getAdapter().notifyItemChanged(position);
    }
    
    // Nested replies methods
    public void showReplyDialog(CommentsModel parentComment) {
        if (mUser == null || !mUser.isPublicAccount()) {
            showNeedPublicProfileDialog();
            return;
        }
        
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reply_comment, null);
        dialog.setContentView(dialogView);
        
        TextView parentPreview = dialogView.findViewById(R.id.parentCommentPreview);
        String previewText = parentComment.getText();
        if (previewText.length() > 50) {
            previewText = previewText.substring(0, 50) + "...";
        }
        parentPreview.setText("Replying to @" + parentComment.getUsername() + ": " + previewText);
        
        EditText replyInput = dialogView.findViewById(R.id.replyInput);
        
        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.postReplyButton).setOnClickListener(v -> {
            String replyText = replyInput.getText().toString().trim();
            if (replyText.isEmpty()) {
                Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            postReply(parentComment, replyText);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void postReply(CommentsModel parentComment, String replyText) {
        // Null safety checks
        if (currentThreadModel == null || currentThreadModel.getID() == null) {
            Toast.makeText(this, "Thread not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (parentComment == null || parentComment.getId() == null || parentComment.getId().isEmpty()) {
            Toast.makeText(this, "Invalid parent comment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String replyId = mThreadsDatabaseReference
            .child(currentThreadModel.getID())
            .child("comments")
            .push().getKey();
        
        if (replyId == null) {
            Toast.makeText(this, "Failed to generate reply ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        CommentsModel reply = new CommentsModel(
            replyText,
            mUser.getUid(),
            replyId,
            Utils.getNowInMillis() + "",
            mUser.getUsername(),
            mUser.getProfileImage()
        );
        
        reply.setParentCommentId(parentComment.getId());
        reply.setDepth(Math.min(parentComment.getDepth() + 1, 3));
        
        mThreadsDatabaseReference
            .child(currentThreadModel.getID())
            .child("comments")
            .child(replyId)
            .setValue(reply);
        
        if (parentComment.getReplyIds() == null) {
            parentComment.setReplyIds(new ArrayList<>());
        }
        parentComment.getReplyIds().add(replyId);
        
        mThreadsDatabaseReference
            .child(currentThreadModel.getID())
            .child("comments")
            .child(parentComment.getId())
            .setValue(parentComment);
        
        Toast.makeText(this, "Reply posted!", Toast.LENGTH_SHORT).show();
    }

    public class CommentsImagesListAdapter extends RecyclerView.Adapter<CommentsImagesListAdapter.ViewHolder> {

        public ArrayList<CommentsModel> data;

        public CommentsImagesListAdapter(){}

        public CommentsImagesListAdapter(ArrayList<CommentsModel> data){
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comments_layout, null);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new CommentsImagesListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CommentsModel comment = data.get(position);
            
            // Apply indentation based on depth
            int marginStart = comment.getDepth() * 32; // 32dp per level
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            params.leftMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, marginStart, getResources().getDisplayMetrics());
            holder.itemView.setLayoutParams(params);
            
            // Set comment content  
            ((TextView)holder.itemView.findViewById(R.id.username)).setText(comment.getUsername());
            ((TextView)holder.itemView.findViewById(R.id.title)).setText(comment.getText());
            TextView time = holder.itemView.findViewById(R.id.time);
            
            // Safe time parsing
            if (comment.getTime() != null && !comment.getTime().isEmpty()) {
                try {
                    time.setText(Utils.calculateTimeDiff(Long.parseLong(comment.getTime())));
                } catch (NumberFormatException e) {
                    time.setText("");
                }
            } else {
                time.setText("");
            }
            
            TextView likes = holder.itemView.findViewById(R.id.likes);
            likes.setText(String.valueOf(comment.getLikes() == null?0:comment.getLikes().size()));
            
            // Like button UI and click handler
            ImageView likeImage = holder.itemView.findViewById(R.id.likeImage);
            LinearLayout likeLayout = holder.itemView.findViewById(R.id.linearLayout2);
            
            // Set like icon state
            if (mUser != null && comment.getLikes() != null && comment.getLikes().contains(mUser.getUid())) {
                likeImage.setImageResource(R.drawable.favorite_24px);
                likeImage.setColorFilter(getResources().getColor(R.color.red));
            } else {
                likeImage.setImageResource(R.drawable.favorite_outline_24px);
                likeImage.setColorFilter(getResources().getColor(R.color.textSec));
            }
            
            // Like click listener
            likeLayout.setOnClickListener(v -> handleCommentLike(comment, position));
            
            // Reply button
            TextView replyButton = holder.itemView.findViewById(R.id.replyButton);
            replyButton.setOnClickListener(v -> {
                if (comment.getDepth() >= 3) {
                    Toast.makeText(ThreadViewActivity.this, 
                        "Maximum nesting level reached", Toast.LENGTH_SHORT).show();
                    return;
                }
                showReplyDialog(comment);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
