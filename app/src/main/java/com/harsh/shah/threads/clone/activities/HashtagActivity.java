package com.harsh.shah.threads.clone.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.databinding.ActivityHashtagBinding;
import com.harsh.shah.threads.clone.model.ThreadModel;

import java.util.ArrayList;

/**
 * Activity to display all threads containing a specific hashtag
 */
public class HashtagActivity extends BaseActivity {

    private static final String TAG = "HashtagActivity";
    private ActivityHashtagBinding binding;
    private String hashtag;
    private ArrayList<ThreadModel> threads = new ArrayList<>();
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHashtagBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get hashtag from intent
        hashtag = getIntent().getStringExtra("hashtag");
        if (hashtag == null || hashtag.isEmpty()) {
            finish();
            return;
        }

        setupUI();
        loadThreads();
    }

    private void setupUI() {
        // Set title
        binding.hashtagTitle.setText("#" + hashtag);
        
        // Back button
        binding.backButton.setOnClickListener(v -> finish());
        
        // Setup RecyclerView
        binding.threadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // You'll need to create an adapter similar to HomeFragment's adapter
        // For now, we'll use a placeholder comment
        // TODO: Implement proper adapter
    }

    private void loadThreads() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        mThreadsDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                threads.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ThreadModel thread = dataSnapshot.getValue(ThreadModel.class);
                    if (thread != null && thread.getHashtags() != null 
                            && thread.getHashtags().contains(hashtag)) {
                        threads.add(thread);
                    }
                }
                
                binding.progressBar.setVisibility(View.GONE);
                
                if (threads.isEmpty()) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.threadsRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                    binding.threadsRecyclerView.setVisibility(View.VISIBLE);
                    // TODO: Update adapter
                }
                
                Log.d(TAG, "Found " + threads.size() + " threads with hashtag #" + hashtag);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading threads: " + error.getMessage());
                showToast("Error loading threads: " + error.getMessage());
            }
        });
    }
}
