package com.harsh.shah.threads.clone.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.HashMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityNewThreadBinding;
import com.harsh.shah.threads.clone.model.PollOptions;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.model.UserModel;
import com.harsh.shah.threads.clone.utils.MDialogUtil;
import com.harsh.shah.threads.clone.utils.TextFormatter;
import com.harsh.shah.threads.clone.utils.Utils;

import com.harsh.shah.threads.clone.database.StorageHelper;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.UUID;

public class NewThreadActivity extends BaseActivity {

    ActivityNewThreadBinding binding;
    ArrayList<String> data = new ArrayList<>();

    ImagesListAdapter adapter = new ImagesListAdapter(data, (listener, dataList) -> {
        data = dataList;
    });

    ActivityResultLauncher<PickVisualMediaRequest> launcher = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), o -> {
        if (o == null) {
            Toast.makeText(NewThreadActivity.this, "No image Selected", Toast.LENGTH_SHORT).show();
        } else {
            if (o.isEmpty()) return;
            for (Uri uri : o) {
                if (data.size() < 6) {
                    if (uri.getPath() == null) continue;
                    
                    // Take persistent URI permission to avoid SecurityException
                    try {
                        getContentResolver().takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException e) {
                        Log.e("NewThreadActivity", "Failed to take persistable URI permission", e);
                        // Continue anyway - some URIs may not support it
                    }
                    
                    data.add(uri.toString());
                    binding.recyclerView.setAdapter(new ImagesListAdapter(data));
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewThreadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.edittext.requestFocus();

        if (mUser != null) {
            binding.textView.setText(mUser.getUsername());
        } else {
            binding.textView.setText("");
            fetchUser();
        }

        binding.insertImage.setOnClickListener(view -> {
            if (data.size() == 5)
                return;
            launcher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        binding.insertPoll.setOnClickListener(view -> {
            // Toggle poll visibility - hide images if poll is shown
            if (binding.pollLayout.getVisibility() == View.GONE) {
                binding.pollLayout.setVisibility(View.VISIBLE);
                binding.constraintLayout2.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.GONE);
                data.clear();
                adapter.notifyDataSetChanged();
            }
        });
        binding.pollRemove.setOnClickListener(view -> {
            binding.pollLayout.setVisibility(View.GONE);
            binding.constraintLayout2.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            // Clear poll options
            binding.pollOption1Edittext.setText("");
            binding.pollOption2Edittext.setText("");
            binding.pollOption3Edittext.setText("");
            binding.pollOption4Edittext.setText("");
        });

        binding.pollOption3Edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().trim().isEmpty()) {
                    binding.pollOption4Edittext.setVisibility(View.GONE);
                } else {
                    binding.pollOption4Edittext.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.postButton.setOnClickListener(view -> {
            // Validate: need either text, images, or poll
            boolean hasText = !binding.edittext.getText().toString().trim().isEmpty();
            boolean hasImages = adapter.getData() != null && !adapter.getData().isEmpty();
            boolean hasPoll = binding.pollLayout.getVisibility() == View.VISIBLE;
            
            if (!hasText && !hasImages && !hasPoll) {
                Toast.makeText(this, "Please add some content", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // If poll is visible, validate poll options
            if (hasPoll) {
                String option1 = binding.pollOption1Edittext.getText().toString().trim();
                String option2 = binding.pollOption2Edittext.getText().toString().trim();
                
                if (option1.isEmpty() || option2.isEmpty()) {
                    Toast.makeText(this, "Please provide at least 2 poll options", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            postThread();
        });
    }

    public void askAndPressback(View view) {
        if (binding.edittext.getText().toString().trim().isEmpty())
            if ((adapter.getData() == null || adapter.getData().isEmpty())) {
                finish();
                return;
            }

        MDialogUtil mDialogUtil = new MDialogUtil(this)
                .setTitle("Are you sure you want to exit?")
                .setMessage("", false);
        AlertDialog dialog = mDialogUtil.create();
        mDialogUtil.setB1("Exit", v -> {
            dialog.dismiss();
            finish();
        });
        mDialogUtil.setB2("Cancel", v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchUser() {
        fetchCurrentUser(user -> {
            if (user != null) {
                binding.textView.setText(user.getUsername());
            }
        });
    }

    private void postThread() {
        if (mUser == null) {
            Toast.makeText(this, "User data not loaded yet. Please wait...", Toast.LENGTH_SHORT).show();
            fetchUser();
            return;
        }

        String threadText = binding.edittext.getText().toString();
        
        // Extract URL from text
        String url = extractUrl(threadText);
        
        // Use LinkPreviewUtil if URL exists AND no images are selected (link preview usually secondary to images)
        if (url != null && !url.isEmpty() && adapter.getData().isEmpty()) {
            showProgressDialog(); // Show dialog while fetching preview
            com.harsh.shah.threads.clone.utils.LinkPreviewUtil.fetchPreview(url, new com.harsh.shah.threads.clone.utils.LinkPreviewUtil.PreviewCallback() {
                @Override
                public void onPreviewLoaded(com.harsh.shah.threads.clone.utils.LinkPreviewUtil.LinkPreview preview) {
                    // Passed to upload/save
                    saveThreadWithPreview(preview);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("NewThreadActivity", "Error fetching preview", e);
                    // Continue without preview
                    saveThreadWithPreview(null);
                }
            });
        } else {
            // Normal flow
             ArrayList<String> localUris = adapter.getData();
            if (localUris != null && !localUris.isEmpty()) {
                uploadImages(localUris);
            } else {
                saveThreadToFirebase(new ArrayList<>());
            }
        }
    }

    private String extractUrl(String text) {
        java.util.regex.Matcher m = android.util.Patterns.WEB_URL.matcher(text);
        if (m.find()) {
            return m.group();
        }
        return null;
    }
    
    private void saveThreadWithPreview(com.harsh.shah.threads.clone.utils.LinkPreviewUtil.LinkPreview preview) {
        ArrayList<String> remoteUrls = new ArrayList<>(); // Empty since we handled image case separately or in normal flow
        // But wait, if we are here, we know adapter.getData() is empty
        
        saveThreadToFirebase(remoteUrls, preview);
    }

    private void uploadImages(ArrayList<String> localUris) {
        showProgressDialog();
        ArrayList<String> remoteUrls = new ArrayList<>();
        processUpload(localUris, 0, remoteUrls);
    }

    private void processUpload(ArrayList<String> localUris, int index, ArrayList<String> remoteUrls) {
        if (index >= localUris.size()) {
            // All uploads finished
            saveThreadToFirebase(remoteUrls);
            return;
        }

        String uriString = localUris.get(index);
        try {
            Uri uri = Uri.parse(uriString);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                processUpload(localUris, index + 1, remoteUrls);
                return;
            }

            java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] bytes = byteBuffer.toByteArray();
            inputStream.close();

            String fileId = UUID.randomUUID().toString();
            StorageHelper.getInstance(this).uploadFile(bytes, "image_" + index + ".jpg", fileId, new StorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageId) {
                    Log.d("NewThreadActivity", "Successfully uploaded image " + index + " with ID: " + imageId);
                    // Store image ID instead of URL (will be used to load from Database)
                    remoteUrls.add(imageId);
                    processUpload(localUris, index + 1, remoteUrls);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("NewThreadActivity", "Upload failed for image " + index, e);
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    runOnUiThread(() -> android.widget.Toast.makeText(NewThreadActivity.this, "Upload failed: " + errorMsg, android.widget.Toast.LENGTH_LONG).show());
                    processUpload(localUris, index + 1, remoteUrls);
                }
            });

        } catch (Exception e) {
            Log.e("NewThreadActivity", "Error processing image " + index, e);
            processUpload(localUris, index + 1, remoteUrls);
        }
    }

    private void saveThreadToFirebase(ArrayList<String> remoteUrls) {
        saveThreadToFirebase(remoteUrls, null);
    }

    private void saveThreadToFirebase(ArrayList<String> remoteUrls, com.harsh.shah.threads.clone.utils.LinkPreviewUtil.LinkPreview preview) {
        final String pid = mThreadsDatabaseReference.push().getKey();
        final String threadText = binding.edittext.getText().toString();

        // Parse hashtags and mentions from text
        ArrayList<String> hashtags = new ArrayList<>(TextFormatter.parseHashtags(threadText));
        ArrayList<String> mentions = new ArrayList<>(TextFormatter.parseMentions(threadText));
        
        // Check if poll is active and collect poll data
        boolean isPoll = binding.pollLayout.getVisibility() == View.VISIBLE;
        PollOptions pollOptions;
        
        if (isPoll) {
            String option1Text = binding.pollOption1Edittext.getText().toString().trim();
            String option2Text = binding.pollOption2Edittext.getText().toString().trim();
            String option3Text = binding.pollOption3Edittext.getText().toString().trim();
            String option4Text = binding.pollOption4Edittext.getText().toString().trim();
            
            PollOptions.PollOptionsItem opt1 = new PollOptions.PollOptionsItem(new ArrayList<>(), option1Text, !option1Text.isEmpty());
            PollOptions.PollOptionsItem opt2 = new PollOptions.PollOptionsItem(new ArrayList<>(), option2Text, !option2Text.isEmpty());
            PollOptions.PollOptionsItem opt3 = new PollOptions.PollOptionsItem(new ArrayList<>(), option3Text, !option3Text.isEmpty());
            PollOptions.PollOptionsItem opt4 = new PollOptions.PollOptionsItem(new ArrayList<>(), option4Text, !option4Text.isEmpty());
            
            pollOptions = new PollOptions(opt1, opt2, opt3, opt4);
        } else {
            // Empty poll data
            pollOptions = new PollOptions(
                new PollOptions.PollOptionsItem(new ArrayList<>(), "", false),
                new PollOptions.PollOptionsItem(new ArrayList<>(), "", false),
                new PollOptions.PollOptionsItem(new ArrayList<>(), "", false),
                new PollOptions.PollOptionsItem(new ArrayList<>(), "", false)
            );
        }

        ThreadModel threadModel = new ThreadModel(
                remoteUrls,
                new HashMap<>(),
                true,
                false,
                isPoll,  // Set isPoll flag
                new ArrayList<>(),
                mUser.getUid(),
                "",
                pid,
                threadText,
                Utils.getNowInMillis() + "",
                pollOptions,  // Use collected poll data
                new ArrayList<>(),
                mUser.getProfileImage(),
                mUser.getUsername(),
                new ArrayList<>()
        );
        
        // Set hashtags and mentions
        threadModel.setHashtags(hashtags);
        threadModel.setMentions(mentions);
        
        // Set Link Preview Data
        if (preview != null) {
            threadModel.setLinkUrl(preview.url);
            threadModel.setLinkTitle(preview.title);
            threadModel.setLinkDescription(preview.description);
            threadModel.setLinkImage(preview.imageUrl);
        }
        
        if (remoteUrls.isEmpty()) showProgressDialog(); // Only show if not already showing from upload

        mThreadsDatabaseReference.child(pid).setValue(threadModel).addOnCompleteListener(task -> {
            hideProgressDialog();
            if (!task.isSuccessful()) {
                showToast(task.getException().toString());
            } else {
                Log.d("NewThreadActivity", "Thread saved successfully with " + remoteUrls.size() + " remote images");
                Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    interface dataChangeListener {
        void onChanged(dataChangeListener dataChangeListener, ArrayList<String> data);
    }

    static class ImagesListAdapter extends RecyclerView.Adapter<ImagesListAdapter.ViewHolder> {

        private final ArrayList<String> data;
        private final dataChangeListener dataChangeListener;

        public ImagesListAdapter(ArrayList<String> data) {
            this.data = data;
            this.dataChangeListener = (listener, data1) -> {
            };
        }

        public ImagesListAdapter(ArrayList<String> data, dataChangeListener listener) {
            this.data = data;
            this.dataChangeListener = listener;
        }

        public ArrayList<String> getData() {
            return data;
        }

        public void setData(ArrayList<String> data) {
            this.data.clear();
            this.data.addAll(data);
            notifyDataSetChanged();
            dataChangeListener.onChanged(this.dataChangeListener, this.data);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(parent.getContext(), R.layout.layout_images_horizontal, null);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            int dp = ((int) (holder.itemView.getContext().getResources().getDisplayMetrics().density));
            if (position == 0) {
                params.setMargins(dp * 100, dp * 8, dp * 8, dp * 8);
            } else {
                params.setMargins(dp * 8, dp * 8, dp * 8, dp * 8);
            }
            holder.shapeableImageView.setLayoutParams(params);

            holder.delete.setOnClickListener(view -> {
                this.data.remove(position);
                dataChangeListener.onChanged(this.dataChangeListener, this.data);
                notifyItemRemoved(position);
                notifyDataSetChanged();
            });
            Uri uri = Uri.parse(data.get(position));
            holder.shapeableImageView.setImageURI(uri);
        }

        public void addData(String data) {
            if (this.data.size() == 5) {
                return;
            }
            this.data.add(data);
            notifyItemInserted(this.data.size());
            dataChangeListener.onChanged(this.dataChangeListener, this.data);
        }

        public void removeData(int position) {
            this.data.remove(position);
            notifyItemRemoved(position);
            dataChangeListener.onChanged(this.dataChangeListener, this.data);
        }

        public void clearData() {
            this.data.clear();
            notifyDataSetChanged();
            dataChangeListener.onChanged(this.dataChangeListener, this.data);

        }

        public void updateData(int position, String data) {
            this.data.set(position, data);
            notifyItemChanged(position);
            dataChangeListener.onChanged(this.dataChangeListener, this.data);

        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            public ShapeableImageView shapeableImageView, delete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                shapeableImageView = itemView.findViewById(R.id.shapeableImageView);
                delete = itemView.findViewById(R.id.delete);
            }
        }
    }
}