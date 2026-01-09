package com.harsh.shah.threads.clone.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.activities.ThreadViewActivity;
import com.harsh.shah.threads.clone.model.CommentsModel;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import io.getstream.photoview.dialog.PhotoViewDialog;
import com.harsh.shah.threads.clone.utils.ImageLoader;

public class ProfileThreadsFragment extends Fragment {

    private static final String ARG_MODE = "mode";
    public static final int MODE_THREADS = 0;
    public static final int MODE_REPLIES = 1;
    public static final int MODE_REPOSTS = 2;

    private int mode;
    private RecyclerView recyclerView;
    private TextView noDataText;
    private Adapter adapter;
    private ArrayList<ThreadModel> data = new ArrayList<>();

    public ProfileThreadsFragment() {
        // Required empty public constructor
    }

    public static ProfileThreadsFragment newInstance(int mode) {
        ProfileThreadsFragment fragment = new ProfileThreadsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getInt(ARG_MODE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        noDataText = view.findViewById(R.id.no_data_text);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new Adapter(data);
        recyclerView.setAdapter(adapter);

        fetchData();
    }

    private void fetchData() {
        if (BaseActivity.mUser == null) return;

        BaseActivity.mThreadsDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                data.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ThreadModel model = dataSnapshot.getValue(ThreadModel.class);
                    if (model != null) {
                        model.setID(dataSnapshot.getKey());
                        boolean include = false;
                        switch (mode) {
                            case MODE_THREADS:
                                if (model.getUserId() != null && model.getUserId().equals(BaseActivity.mUser.getUid())) {
                                    include = true;
                                }
                                break;
                            case MODE_REPLIES:
                                if (model.getComments() != null) {
                                    for (CommentsModel comment : model.getComments().values()) {
                                        if (comment.getUserId() != null && comment.getUserId().equals(BaseActivity.mUser.getUid())) {
                                            include = true;
                                            break;
                                        }
                                    }
                                }
                                break;
                            case MODE_REPOSTS:
                                if (model.getReposts() != null && model.getReposts().contains(BaseActivity.mUser.getUid())) {
                                    include = true;
                                }
                                break;
                        }
                        if (include) {
                            data.add(0, model); // Add to top
                        }
                    }
                }
                
                if (data.isEmpty()) {
                    noDataText.setVisibility(View.VISIBLE);
                    if (mode == MODE_THREADS) noDataText.setText("No threads yet.");
                    else if (mode == MODE_REPLIES) noDataText.setText("No replies yet.");
                    else noDataText.setText("No reposts yet.");
                } else {
                    noDataText.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // Copied and adapted from HomeFragment
    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private ArrayList<ThreadModel> data;

        public Adapter(ArrayList<ThreadModel> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.home_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ThreadModel model = data.get(position);
            
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

            try {
                ((TextView) holder.itemView.findViewById(R.id.time)).setText(Utils.calculateTimeDiff(Long.parseLong(model.getTime())));
            } catch (Exception e) {
                ((TextView) holder.itemView.findViewById(R.id.time)).setText("");
            }
            ((TextView) holder.itemView.findViewById(R.id.title)).setText(model.getText() != null ? model.getText() : "");

            int likesCount = model.getLikes() != null ? model.getLikes().size() : 0;
            int commentsCount = model.getComments() != null ? model.getComments().size() : 0;
            int repostsCount = model.getReposts() != null ? model.getReposts().size() : 0;

            ((TextView) holder.itemView.findViewById(R.id.likes)).setText(String.valueOf(likesCount));
            ((TextView) holder.itemView.findViewById(R.id.comments)).setText(String.valueOf(commentsCount));
            ((TextView) holder.itemView.findViewById(R.id.reposts)).setText(String.valueOf(repostsCount));

            RecyclerView imagesList = holder.itemView.findViewById(R.id.imagesListRecyclerView);
            imagesList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

            if (model.getImages() != null && !model.getImages().isEmpty()) {
                imagesList.setVisibility(View.VISIBLE);
                imagesList.setAdapter(new PostImagesListAdapter(model.getImages()));
            } else {
                imagesList.setVisibility(View.GONE);
            }

            // Poll logic
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

                // Poll voting logic
                if (BaseActivity.mUser != null) {
                    String userId = BaseActivity.mUser.getUid();
                    
                    // Check if user has already voted
                    boolean hasVoted = false;
                    if (model.getPollOptions().getOption1() != null && model.getPollOptions().getOption1().getVotes().contains(userId)) hasVoted = true;
                    if (model.getPollOptions().getOption2() != null && model.getPollOptions().getOption2().getVotes().contains(userId)) hasVoted = true;
                    if (model.getPollOptions().getOption3() != null && model.getPollOptions().getOption3().getVotes().contains(userId)) hasVoted = true;
                    if (model.getPollOptions().getOption4() != null && model.getPollOptions().getOption4().getVotes().contains(userId)) hasVoted = true;

                    if (!hasVoted) {
                        holder.itemView.findViewById(R.id.poll_option_1).setOnClickListener(v -> votePoll(holder.itemView.getContext(), model, 1, position));
                        holder.itemView.findViewById(R.id.poll_option_2).setOnClickListener(v -> votePoll(holder.itemView.getContext(), model, 2, position));
                        holder.itemView.findViewById(R.id.poll_option_3).setOnClickListener(v -> votePoll(holder.itemView.getContext(), model, 3, position));
                        holder.itemView.findViewById(R.id.poll_option_4).setOnClickListener(v -> votePoll(holder.itemView.getContext(), model, 4, position));
                    } else {
                        // Show results logic similar to HomeFragment could be added here
                        // For now we just disable clicks to prevent re-voting
                        holder.itemView.findViewById(R.id.poll_option_1).setOnClickListener(v -> android.widget.Toast.makeText(holder.itemView.getContext(), "You've already voted", android.widget.Toast.LENGTH_SHORT).show());
                        holder.itemView.findViewById(R.id.poll_option_2).setOnClickListener(v -> android.widget.Toast.makeText(holder.itemView.getContext(), "You've already voted", android.widget.Toast.LENGTH_SHORT).show());
                        holder.itemView.findViewById(R.id.poll_option_3).setOnClickListener(v -> android.widget.Toast.makeText(holder.itemView.getContext(), "You've already voted", android.widget.Toast.LENGTH_SHORT).show());
                        holder.itemView.findViewById(R.id.poll_option_4).setOnClickListener(v -> android.widget.Toast.makeText(holder.itemView.getContext(), "You've already voted", android.widget.Toast.LENGTH_SHORT).show());
                    }
                }
            } else {
                holder.itemView.findViewById(R.id.poll_layout).setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(view -> startActivity(new Intent(getContext(), ThreadViewActivity.class).putExtra("thread", model.getID())));

            holder.itemView.findViewById(R.id.likeThreadLayout).setOnClickListener(view -> {
                if (model.getLikes() == null || BaseActivity.mUser == null) return;

                if (model.getLikes().contains(BaseActivity.mUser.getUid())) {
                    model.getLikes().remove(BaseActivity.mUser.getUid());
                } else {
                    model.getLikes().add(BaseActivity.mUser.getUid());
                }
                setLikeStatus(holder, model);
                BaseActivity.mThreadsDatabaseReference.child(model.getID()).setValue(model);
                notifyItemChanged(position);
            });
            setLikeStatus(holder, model);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private void setLikeStatus(ViewHolder holder, ThreadModel model) {
            if (model == null || model.getLikes() == null || BaseActivity.mUser == null) return;

            if (model.getLikes().contains(BaseActivity.mUser.getUid())) {
                ((ImageView) holder.itemView.findViewById(R.id.likeImage)).setImageResource(R.drawable.favorite_24px);
            } else {
                ((ImageView) holder.itemView.findViewById(R.id.likeImage)).setImageResource(R.drawable.favorite_outline_24px);
                ((ImageView) holder.itemView.findViewById(R.id.likeImage)).setColorFilter(getResources().getColor(R.color.red));
            }
        }

        private void votePoll(android.content.Context context, ThreadModel thread, int optionNumber, int position) {
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
                if (context != null) {
                    android.widget.Toast.makeText(context, "You've already voted on this poll", android.widget.Toast.LENGTH_SHORT).show();
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
                    if (context != null) {
                        android.widget.Toast.makeText(context, "Vote recorded!", android.widget.Toast.LENGTH_SHORT).show();
                    }
                    // Update UI
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        android.widget.Toast.makeText(context, "Failed to record vote", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }

    }
    }

    public static class PostImagesListAdapter extends RecyclerView.Adapter<PostImagesListAdapter.ViewHolder> {
        private List<String> data;

        public PostImagesListAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_list_item_image_item, parent, false); // Using parent, false
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImageView imageView = holder.itemView.findViewById(R.id.imageView);
            imageView.setImageDrawable(null); // Clear recycled image
            if (!data.get(position).isEmpty()) {
                ImageLoader.loadImageOrUrl(imageView, data.get(position));
            }
            imageView.setOnClickListener(view -> {
                 new PhotoViewDialog.Builder(holder.itemView.getContext(), data, (imageView1, url) -> ImageLoader.loadImageOrUrl(imageView1, (String) url)).withTransitionFrom(imageView).withStartPosition(position).build().show(true);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
