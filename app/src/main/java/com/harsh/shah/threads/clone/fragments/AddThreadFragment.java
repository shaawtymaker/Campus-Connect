package com.harsh.shah.threads.clone.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.model.PollOptions;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.model.UserModel;
import com.harsh.shah.threads.clone.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AddThreadFragment extends Fragment {

    private static AddThreadFragment instance;
    private ArrayList<String> data = new ArrayList<>();
    private ImagesListAdapter adapter;
    private ActivityResultLauncher<PickVisualMediaRequest> launcher;
    
    private EditText threadEditText;
    private TextView usernameView, postButton;
    private ShapeableImageView profileImage;
    private RecyclerView imagesRecyclerView;
    private ImageView insertImage;

    public AddThreadFragment() {
        // Required empty public constructor
    }

    public static AddThreadFragment newInstance() {
        return new AddThreadFragment();
    }

    public static AddThreadFragment getInstance(){
        if(instance == null)
            instance = new AddThreadFragment();
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launcher = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), o -> {
            if (o == null || o.isEmpty()) {
                Toast.makeText(getContext(), "No image Selected", Toast.LENGTH_SHORT).show();
            } else {
                for (Uri uri : o) {
                    if (data.size() < 6) {
                        data.add(uri.toString());
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_thread, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        threadEditText = view.findViewById(R.id.threadEditText);
        usernameView = view.findViewById(R.id.textView);
        profileImage = view.findViewById(R.id.shapeableImageView);
        postButton = view.findViewById(R.id.postButton);
        imagesRecyclerView = view.findViewById(R.id.imagesRecyclerView);
        insertImage = view.findViewById(R.id.insertImage);

        adapter = new ImagesListAdapter(data);
        imagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        imagesRecyclerView.setAdapter(adapter);

        if (BaseActivity.mUser != null) {
            usernameView.setText(BaseActivity.mUser.getUsername());
            if (BaseActivity.mUser.getProfileImage() != null && !BaseActivity.mUser.getProfileImage().isEmpty()) {
                Picasso.get().load(BaseActivity.mUser.getProfileImage()).placeholder(R.drawable.person_outline_24px).into(profileImage);
            }
        } else {
            fetchUser();
        }

        insertImage.setOnClickListener(v -> {
            if (data.size() >= 5) return;
            launcher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        postButton.setOnClickListener(v -> postThread());
    }

    private void fetchUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(firebaseUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            UserModel user = snapshot.getValue(UserModel.class);
                            if (user != null) {
                                BaseActivity.mUser = user;
                                usernameView.setText(BaseActivity.mUser.getUsername());
                                if (BaseActivity.mUser.getProfileImage() != null && !BaseActivity.mUser.getProfileImage().isEmpty()) {
                                    Picasso.get().load(BaseActivity.mUser.getProfileImage()).placeholder(R.drawable.person_outline_24px).into(profileImage);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void postThread() {
        if (threadEditText.getText().toString().trim().isEmpty() && data.isEmpty()) return;
        if (BaseActivity.mUser == null) {
            Toast.makeText(getContext(), "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String pid = BaseActivity.mThreadsDatabaseReference.push().getKey();
        ThreadModel threadModel = new ThreadModel(
                data,
                new ArrayList<>(),
                true,
                false,
                false,
                new ArrayList<>(),
                BaseActivity.mUser.getUid(),
                "",
                pid,
                threadEditText.getText().toString(),
                Utils.getNowInMillis() + "",
                new PollOptions(new PollOptions.PollOptionsItem(new ArrayList<>(), "", false), new PollOptions.PollOptionsItem(new ArrayList<>(), "", false), new PollOptions.PollOptionsItem(new ArrayList<>(), "", false), new PollOptions.PollOptionsItem(new ArrayList<>(), "", false)),
                new ArrayList<>(),
                BaseActivity.mUser.getProfileImage(),
                BaseActivity.mUser.getUsername(),
                new ArrayList<>()
        );

        BaseActivity.mThreadsDatabaseReference.child(pid).setValue(threadModel).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Thread posted!", Toast.LENGTH_SHORT).show();
                threadEditText.setText("");
                data.clear();
                adapter.notifyDataSetChanged();
                // Optionally switch to Home tab
            } else {
                Toast.makeText(getContext(), "Failed to post", Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class ImagesListAdapter extends RecyclerView.Adapter<ImagesListAdapter.ViewHolder> {
        private final ArrayList<String> data;

        public ImagesListAdapter(ArrayList<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_images_horizontal, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Uri uri = Uri.parse(data.get(position));
            holder.shapeableImageView.setImageURI(uri);
            holder.delete.setOnClickListener(v -> {
                data.remove(position);
                notifyItemRemoved(position);
                notifyDataSetChanged(); // Refresh indices
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView shapeableImageView, delete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                shapeableImageView = itemView.findViewById(R.id.shapeableImageView);
                delete = itemView.findViewById(R.id.delete);
            }
        }
    }
}