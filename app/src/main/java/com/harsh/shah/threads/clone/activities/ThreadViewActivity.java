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
    }

    private void addNewComment() {
        if (currentThreadModel == null) {
            Toast.makeText(this, "Thread not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, ReplyToThreadActivity.class).putExtra("threadModel", currentThreadModel));
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

        if(threadModel.isIsPoll() && false) {
            binding.pollLayout.setVisibility(View.VISIBLE);
            binding.postRecyclerView.setVisibility(View.GONE);
        }
        else {
            binding.pollLayout.setVisibility(View.GONE);
            binding.postRecyclerView.setVisibility(View.VISIBLE);
        }

        if(threadModel.getPollOptions()!=null){
            binding.pollOption1.setText(threadModel.getPollOptions().getOption1().getText());
            binding.pollOption2.setText(threadModel.getPollOptions().getOption2().getText());
            if (threadModel.getPollOptions().getOption3().getVisibility()) {
                binding.pollOption3.setVisibility(View.VISIBLE);
                binding.pollOption3.setText(threadModel.getPollOptions().getOption3().getText());
            }
            if (threadModel.getPollOptions().getOption4().getVisibility()) {
                binding.pollOption4.setVisibility(View.VISIBLE);
                binding.pollOption4.setText(threadModel.getPollOptions().getOption4().getText());
            }

            binding.pollOption1.setOnClickListener(view -> {
                setHeaderPos((TextView) view, true);
                setHeaderPos((TextView) binding.pollOption2, false);
                setHeaderPos((TextView) binding.pollOption3, false);
                setHeaderPos((TextView) binding.pollOption4, false);
            });
            binding.pollOption2.setOnClickListener(view -> {
                setHeaderPos((TextView) view, true);
                setHeaderPos((TextView) binding.pollOption1, false);
                setHeaderPos((TextView) binding.pollOption3, false);
                setHeaderPos((TextView) binding.pollOption4, false);
            });
            binding.pollOption3.setOnClickListener(view -> {
                setHeaderPos((TextView) view, true);
                setHeaderPos((TextView) binding.pollOption1, false);
                setHeaderPos((TextView) binding.pollOption2, false);
                setHeaderPos((TextView) binding.pollOption4, false);
            });
            binding.pollOption4.setOnClickListener(view -> {
                setHeaderPos((TextView) view, true);
                setHeaderPos((TextView) binding.pollOption1, false);
                setHeaderPos((TextView) binding.pollOption2, false);
                setHeaderPos((TextView) binding.pollOption3, false);
            });
        }

        if(threadModel.getComments()!=null){
            binding.commentsRecyclerView.setAdapter(new CommentsImagesListAdapter(threadModel.getComments()));
        }

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

    private void setHeaderPos(TextView view, boolean isActivated) {
        if (isActivated) {
            view.setBackgroundResource(R.drawable.button_background_outlined_filled);
            view.setTextColor(getResources().getColor(R.color.textMain));
        } else {
            view.setBackgroundResource(R.drawable.button_background_outlined);
            view.setTextColor(getResources().getColor(R.color.textSec));
        }
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
        String replyId = mThreadsDatabaseReference
            .child(currentThreadModel.getID())
            .child("comments")
            .push().getKey();
        
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
            time.setText(Utils.calculateTimeDiff(Long.parseLong(comment.getTime())));
            TextView likes = holder.itemView.findViewById(R.id.likes);
            likes.setText(String.valueOf(comment.getLikes() == null?0:comment.getLikes().size()));
            
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
