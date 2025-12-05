package com.harsh.shah.threads.clone.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityReplyToThreadBinding;
import com.harsh.shah.threads.clone.fragments.HomeFragment;
import com.harsh.shah.threads.clone.model.CommentsModel;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.utils.MDialogUtil;
import com.harsh.shah.threads.clone.utils.Utils;

import java.util.ArrayList;

public class ReplyToThreadActivity extends BaseActivity {

    ActivityReplyToThreadBinding binding;

    ThreadModel threadModel;

    ArrayList<String> data = new ArrayList<>();
    NewThreadActivity.ImagesListAdapter adapter = new NewThreadActivity.ImagesListAdapter(data, (listener, dataList) -> {
        data = dataList;
    });
    ActivityResultLauncher<PickVisualMediaRequest> launcher = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), o -> {
        if (o == null) {
            Toast.makeText(ReplyToThreadActivity.this, "No image Selected", Toast.LENGTH_SHORT).show();
        } else {
            for (Uri uri : o) {
                if (data.size() < 6) {
                    data.add(uri.toString());
                    binding.replyImagesRecyclerView.setAdapter(new NewThreadActivity.ImagesListAdapter(data));
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityReplyToThreadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent().hasExtra("threadModel")) {
            threadModel = getIntent().getParcelableExtra("threadModel");
            android.util.Log.d("ReplyToThread", "Received threadModel with ID: " + (threadModel != null ? threadModel.getID() : "null model"));
        }

        if (threadModel == null) {
            Toast.makeText(this, "Error loading thread", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.imagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.replyImagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        binding.insertImage.setOnClickListener(v -> {
            if (data.size() >= 5) return;
            launcher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.postButton.setOnClickListener(v -> {
            if (binding.edittext.getText().toString().trim().isEmpty() && data.isEmpty()) return;
            if (mUser == null) {
                Toast.makeText(this, "User data not loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            String pid = mThreadsDatabaseReference.push().getKey();
            ThreadModel replyThread = new ThreadModel(
                    data,
                    new HashMap<>(),
                    true,
                    false,
                    false,
                    new ArrayList<>(),
                    mUser.getUid(),
                    "",
                    pid,
                    binding.edittext.getText().toString(),
                    Utils.getNowInMillis() + "",
                    null,
                    new ArrayList<>(),
                    mUser.getProfileImage(),
                    mUser.getUsername(),
                    new ArrayList<>()
            );

            // Add reply to the original thread's comments
            if (threadModel.getComments() == null) threadModel.setComments(new HashMap<>());
            CommentsModel comment = new CommentsModel(
                    binding.edittext.getText().toString(),
                    mUser.getUid(),
                    pid,
                    Utils.getNowInMillis() + "",
                    mUser.getUsername(),
                    mUser.getProfileImage()
            );
            threadModel.getComments().put(comment.getId(), comment);

            // Update original thread
            if (threadModel.getID() != null && !threadModel.getID().isEmpty()) {
                mThreadsDatabaseReference.child(threadModel.getID()).setValue(threadModel);
                Toast.makeText(this, "Reply posted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Thread ID is missing", Toast.LENGTH_SHORT).show();
            }
            
            finish();
        });

        binding.username.setText(threadModel.getUsername());
        
        // Safe time parsing
        if (threadModel.getTime() != null && !threadModel.getTime().isEmpty()) {
            try {
                binding.time.setText(Utils.calculateTimeDiff(Long.parseLong(threadModel.getTime())));
            } catch (NumberFormatException e) {
                binding.time.setText("");
            }
        } else {
            binding.time.setText("");
        }

        binding.imagesRecyclerView.setAdapter(new HomeFragment.PostImagesListAdapter(threadModel.getImages(), false));

        if(threadModel.isIsPoll() && false) {
            binding.pollLayout.setVisibility(View.VISIBLE);
            binding.imagesRecyclerView.setVisibility(View.GONE);
        }
        else {
            binding.pollLayout.setVisibility(View.GONE);
            binding.imagesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    public void askAndPressback(View view) {
        if (binding.edittext.getText().toString().trim().isEmpty() && data.isEmpty()) {
            onBackPressed();
            return;
        }

        MDialogUtil mDialogUtil = new MDialogUtil(this)
                .setTitle("Are you sure you want to exit?")
                .setMessage("", false);
        AlertDialog dialog = mDialogUtil.create();
        mDialogUtil.setB1("Exit", v -> {
            dialog.dismiss();
            onBackPressed();
        });
        mDialogUtil.setB2("Cancel", v -> dialog.dismiss());
        dialog.show();
    }
}