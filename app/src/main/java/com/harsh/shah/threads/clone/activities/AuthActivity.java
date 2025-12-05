package com.harsh.shah.threads.clone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.harsh.shah.threads.clone.BaseActivity;
import com.harsh.shah.threads.clone.databinding.ActivityAuthBinding;
import com.harsh.shah.threads.clone.model.ThreadModel;
import com.harsh.shah.threads.clone.model.UserModel;

import java.util.ArrayList;

public class AuthActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    ActivityAuthBinding binding;
    private boolean isModeRegister = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.password.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                hideKeyboard(view);
            }
        });

        binding.password.setOnEditorActionListener((textView, i, keyEvent) -> {
            hideKeyboard(textView);
            binding.signIn.performClick();
            return false;
        });

        addTextWatchers();
        initOnClickListeners();
    }

    private void initOnClickListeners() {
        binding.signIn.setOnClickListener(view -> {
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString().trim();
            String username = binding.username.getText().toString().trim().toLowerCase();
            binding.username.setText(username);

            if (isModeRegister) {
                if (binding.usernameLayout.getError() == null && binding.emailLayout.getError() == null && binding.passwordLayout.getError() == null) {
                    getUsersDatabase(new AuthListener() {
                        @Override
                        public void onAuthTaskStart() {
                            showProgressDialog();
                        }

                        @Override
                        public void onAuthSuccess(DataSnapshot snapshot) {
                            //hideProgressDialog();
                            if (snapshot.hasChild(username)) {
                                hideProgressDialog();
                                binding.usernameLayout.setError("Username already taken.");
                                return;
                            }
                            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Log.e(TAG, "onAuthSuccess-Error: ", task.getException());
                                    showToast("Error: " + task.getException());
                                    hideProgressDialog();
                                    return;
                                }
                                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(username).build();
                                task.getResult().getUser().updateProfile(userProfileChangeRequest);
                                task.getResult().getUser().reload().isSuccessful();
                                //showProgressDialog();
                                mUsersDatabaseReference.child(username).setValue(
                                        new UserModel(
                                                new ArrayList<>(),
                                                new ArrayList<>(),
                                                "",
                                                new ArrayList<>(),
                                                true,
                                                "",
                                                task.getResult().getUser().getUid(),
                                                password,
                                                new ArrayList<>(),
                                                new ArrayList<>(),
                                                username,
                                                true,
                                                new ArrayList<>(),
                                                email,
                                                new ArrayList<>(),
                                                username,
                                                ""
                                        )).addOnCompleteListener(task1 -> {
                                    hideProgressDialog();
                                    if(task1.isSuccessful()){
                                        startActivity(new Intent(AuthActivity.this, MainActivity.class));
                                        finish();
                                    } else {
                                        Log.e(TAG, "onAuthSuccess: ", task1.getException());
                                    }
                                });
                            });

                        }

                        @Override
                        public void onAuthFail(DatabaseError error) {
                            hideProgressDialog();
                        }
                    });
                }
            } else {
                if (binding.emailLayout.getError() == null && binding.passwordLayout.getError() == null) {
                    showProgressDialog();
                    if(email.contains("@")) {
                        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                            hideProgressDialog();
                            if (!task.isSuccessful()) {
                                Log.e(TAG, "onAuthSuccess-Error: ", task.getException());
                                showToast("Error: " + task.getException());
                                return;
                            }
                            startActivity(new Intent(AuthActivity.this, MainActivity.class));
                            finish();
                        });
                        //hideProgressDialog();
                    }
                    else{
                        mUsersDatabaseReference.child(email).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                UserModel mUser = snapshot.getValue(UserModel.class);
                                if(mUser != null && mUser.getEmail() != null){
                                    mAuth.signInWithEmailAndPassword(mUser.getEmail(), password).addOnCompleteListener(task -> {
                                        hideProgressDialog();
                                        if (!task.isSuccessful()) {
                                            Log.e(TAG, "onAuthSuccess-Error: ", task.getException());
                                            showToast("Error: " + task.getException());
                                            return;
                                        }
                                        startActivity(new Intent(AuthActivity.this, MainActivity.class));
                                        finish();
                                    });
                                }else{
                                    binding.emailLayout.setError("Invalid Username/Password");
                                    hideProgressDialog();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                hideProgressDialog();
                            }
                        });
                    }
                }
            }
        });

        binding.loginRegister.setOnClickListener(view -> {
            binding.usernameLayout.setVisibility(binding.usernameLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            binding.loginRegister.setText(binding.loginRegister.getText().equals("Register") ? "Login" : "Register");
            binding.signIn.setText(binding.loginRegister.getText().equals("Register") ? "Log in" : "Sign up");
            binding.emailLayout.setHint(binding.loginRegister.getText().equals("Register") ? "Username or email" : "Email");
            isModeRegister = !binding.loginRegister.getText().equals("Register");
        });

        binding.loginWithGoogle.setOnClickListener(view -> {
            startActivityForResult(googleSignInClient.getSignInIntent(), 22);
        });

    }


    private void addTextWatchers() {

        binding.username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                binding.usernameLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateUsername(s.toString());
            }

            private boolean isUsernameValid(String username) {
                String usernameRegex = "^[a-z0-9._]{6,20}$";
                return username.matches(usernameRegex);
            }

            private void validateUsername(String username) {
                if (isUsernameValid(username))
                    binding.usernameLayout.setError(null);
                else if (username.trim().length() > 20 || username.trim().length() < 6)
                    binding.usernameLayout.setError("Username must be between 6 and 20 characters.");
                else if (username.contains(" "))
                    binding.usernameLayout.setError("Username should not contain spaces.");
                else if(username.matches("^.*[A-Z].*$"))
                    binding.usernameLayout.setError("Username should not contain capital characters.");
                else
                    binding.usernameLayout.setError("Contains invalid characters.");
            }
        });

        binding.password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.passwordLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() < 6)
                    binding.passwordLayout.setError("Password must be at least 6 characters.");
                else
                    binding.passwordLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.emailLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

//        binding.email.addTextChangedListener(new TextWatcher() {
//            public final Pattern VALID_EMAIL_ADDRESS_REGEX =
//                    Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
//
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!isModeRegister) {
//                    if (!validate(charSequence.toString()))
//                        binding.emailLayout.setError("Invalid email address.");
//                    else
//                        binding.emailLayout.setError(null);
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//            }
//
//            public boolean validate(String emailStr) {
//                Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
//                return matcher.matches();
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

}