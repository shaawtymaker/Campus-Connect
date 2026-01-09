package com.harsh.shah.threads.clone.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.fragments.HomeFragment;
import com.harsh.shah.threads.clone.model.ThreadModel;

import java.util.ArrayList;
import java.util.List;

public class SavedThreadsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyState;
    private ArrayList<ThreadModel> savedThreads = new ArrayList<>();
    private HomeFragment.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_threads);

        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyState = findViewById(R.id.emptyState);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create HomeFragment instance to access its Adapter
        HomeFragment homeFragment = new HomeFragment();
        adapter = homeFragment.new Adapter(savedThreads);
        recyclerView.setAdapter(adapter);

        // Back button
        findViewById(R.id.back).setOnClickListener(v -> finish());

        // Load user and saved threads
        fetchCurrentUser(user -> {
            if (user != null) {
                mUser = user;
                loadSavedThreads();
            }
        });

        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadSavedThreads();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadSavedThreads() {
        if (mUser == null || mUser.getSavedThreads() == null || mUser.getSavedThreads().isEmpty()) {
            showEmptyState();
            return;
        }

        savedThreads.clear();
        adapter.clearData();
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        List<String> savedThreadIds = mUser.getSavedThreads();

        // Load each saved thread from Firebase
        for (String threadId : savedThreadIds) {
            mThreadsDatabaseReference.child(threadId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ThreadModel thread = snapshot.getValue(ThreadModel.class);
                    if (thread != null) {
                        thread.setID(snapshot.getKey());
                        savedThreads.add(thread);
                        adapter.addData(thread);
                    }

                    // Check if all threads loaded
                    if (savedThreads.isEmpty() && savedThreadIds.indexOf(threadId) == savedThreadIds.size() - 1) {
                        showEmptyState();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.widget.Toast.makeText(SavedThreadsActivity.this,
                        "Error loading saved threads", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}
