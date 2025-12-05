package com.harsh.shah.threads.clone.interfaces.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.harsh.shah.threads.clone.Constants;
import com.harsh.shah.threads.clone.model.UserModel;

public class onProfileUpdateImpl implements onProfileUpdate {

     DatabaseReference mUsersDatabaseReference = FirebaseDatabase.getInstance().getReference(Constants.USERS_DB_REF);


    @Override
    public void setup() {
        mUsersDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    UserModel user = dataSnapshot.getValue(UserModel.class);
                    if(user != null){
                        if(user.getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                            onProfileUpdate(user);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onProfileUpdate(UserModel userModel) {

    }
}
