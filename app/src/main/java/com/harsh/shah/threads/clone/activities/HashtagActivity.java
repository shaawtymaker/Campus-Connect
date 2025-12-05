package com.harsh.shah.threads.clone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.databinding.ActivityHashtagBinding;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.utils.Utils;

import java.util.ArrayList;

/**
 * Activity to display all threads containing a specific hashtag
 */
public class HashtagActivity extends BaseActivity {

    private static final String TAG = "HashtagActivity";
    private ActivityHashtagBinding binding;
    private String hashtag;
    private ArrayList<ThreadModel> threads = new ArrayList<>();
    private ThreadsAdapter adapter;

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
        
        // Setup RecyclerView with simple adapter
        binding.threadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ThreadsAdapter();
        binding.threadsRecyclerView.setAdapter(adapter);
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
                    adapter.notifyDataSetChanged();
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
    
    // Simple adapter for displaying threads
    class ThreadsAdapter extends RecyclerView.Adapter<ThreadsAdapter.ViewHolder> {
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ThreadModel thread = threads.get(position);
            
            // Set username
            if (holder.username != null) {
                holder.username.setText(thread.getUsername() != null ? thread.getUsername() : "");
            }
            
            // Set thread text
            if (holder.text != null) {
                holder.text.setText(thread.getText() != null ? thread.getText() : "");
            }
            
            // Set time
            if (holder.time != null) {
                try {
                    holder.time.setText(Utils.calculateTimeDiff(Long.parseLong(thread.getTime())));
                } catch (Exception e) {
                    holder.time.setText("");
                }
            }
            
            // Click to open thread
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HashtagActivity.this, ThreadViewActivity.class);
                intent.putExtra("thread", thread.getID());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return threads.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView username, text, time;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                text = itemView.findViewById(R.id.title);  // Changed from textPara to title
                time = itemView.findViewById(R.id.time);
            }
        }
    }
}
