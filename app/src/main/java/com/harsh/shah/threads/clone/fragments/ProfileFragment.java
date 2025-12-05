package com.harsh.shah.threads.clone.fragments;

import static com.harsh.shah.threads.clone.BaseActivity.mUser;

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
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.R;
import com.harsh.shah.threads.clone.activities.EditProfileActivity;
import com.harsh.shah.threads.clone.activities.SettingsActivity;
import com.harsh.shah.threads.clone.activities.settings.FollowingFollowersProfilesActivity;
import com.harsh.shah.threads.clone.activities.settings.PrivacyActivity;
import com.harsh.shah.threads.clone.model.UserModel;

public class ProfileFragment extends Fragment {

    private static ProfileFragment instance;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static ProfileFragment getInstance() {
        if (instance == null) {
            instance = new ProfileFragment();
        }
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView segmentImage = view.findViewById(R.id.segmentImage);
        segmentImage.setOnClickListener(view1 -> startActivity(new Intent(getContext(), SettingsActivity.class)));

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        viewPager.setAdapter(new PageAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, ((tab, position) -> tab.setText(position==0?"Threads":position==1?"Replies":"Reposts"))).attach();
        viewPager.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
        });

        initOnClickListeners(view);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    private void initOnClickListeners(View view){
        view.findViewById(R.id.edit_profile).setOnClickListener(view1 -> {
            startActivity(new Intent(getContext(), EditProfileActivity.class));
        });

        view.findViewById(R.id.lockImage).setOnClickListener(v-> startActivity(new Intent(getContext(), PrivacyActivity.class)));

        View.OnClickListener listener = v -> startActivity(new Intent(getContext(), FollowingFollowersProfilesActivity.class));
        view.findViewById(R.id.followers).setOnClickListener(listener);
        view.findViewById(R.id.shapeableImageView4).setOnClickListener(listener);
        view.findViewById(R.id.shapeableImageView3).setOnClickListener(listener);

        final TextView
                name = view.findViewById(R.id.name),
                username = view.findViewById(R.id.username),
                bio = view.findViewById(R.id.bio),
                addedLink = view.findViewById(R.id.addedLink),
                followers = view.findViewById(R.id.followers);

        setupDatabaseListener(name, username, bio, addedLink, followers);
    }

    private void updateUI(TextView name, TextView username, TextView bio, TextView addedLink, TextView followers, UserModel user) {
        name.setText(user.getName());
        if (user.getUsername() != null) username.setText(user.getUsername());
        if (user.getBio() != null) bio.setText(user.getBio());
        addedLink.setText((user.getInfoLink() == null || user.getInfoLink().isEmpty()) ? "" : "• " + user.getInfoLink());
        followers.setText(user.getFollowers() == null ? "0 followers" : user.getFollowers().size() + " followers");
    }

    private void setupDatabaseListener(TextView name, TextView username, TextView bio, TextView addedLink, TextView followers) {
        if (mUser == null) {
            if (getActivity() instanceof BaseActivity) {
                ((BaseActivity) getActivity()).fetchCurrentUser(user -> {
                    if (user != null) {
                        mUser = user;
                        updateUI(name, username, bio, addedLink, followers, mUser);
                        setupDatabaseListener(name, username, bio, addedLink, followers); // Re-attach listener
                    }
                });
            }
            return;
        }

        if (mUser.getUsername() == null) return;

        BaseActivity.mUsersDatabaseReference.child(mUser.getUsername()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null) {
                    mUser = user;
                    updateUI(name, username, bio, addedLink, followers, mUser);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static class PageAdapter extends FragmentStateAdapter {
        public PageAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return ProfileThreadsFragment.newInstance(ProfileThreadsFragment.MODE_THREADS);
            if (position == 1) return ProfileThreadsFragment.newInstance(ProfileThreadsFragment.MODE_REPLIES);
            return ProfileThreadsFragment.newInstance(ProfileThreadsFragment.MODE_REPOSTS);
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}