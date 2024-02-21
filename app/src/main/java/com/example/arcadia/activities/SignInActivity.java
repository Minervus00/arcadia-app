package com.example.arcadia.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.arcadia.R;
import com.example.arcadia.databinding.ActivitySignInBinding;
import com.example.arcadia.utilities.Constants;
import com.example.arcadia.utilities.PreferenceManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.auth.api.signin.SignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignInActivity extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseUser user;
    GoogleSignInClient googleSignInClient;
    String lastName, firstName, encodedImage;

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    private final ActivityResultLauncher <Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult res) {
//            showToast("In result" + res.getResultCode());
            if (res.getResultCode() == Activity.RESULT_OK) {
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(res.getData());
                try {
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                    auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                user = FirebaseAuth.getInstance().getCurrentUser();
                                // Vérifier si email + passwd existe si oui signInGoogle
                                // Sinon SignUpGoogle
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection(Constants.KEY_COLLECTION_USERS)
                                        .whereEqualTo(Constants.KEY_EMAIL, user.getEmail())
                                        .whereEqualTo(Constants.KEY_GOOGLE_USER, 1)
                                        .get()
                                        .addOnCompleteListener(t -> {
                                            if (t.isSuccessful() && t.getResult() != null
                                                    && t.getResult().getDocuments().size() > 0) { // email google user existe => signin
                                                DocumentSnapshot docSnapshot = t.getResult().getDocuments().get(0);
                                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                                preferenceManager.putString(Constants.KEY_USER_ID, docSnapshot.getId());
                                                preferenceManager.putString(Constants.KEY_NAME, docSnapshot.getString(Constants.KEY_NAME));
                                                preferenceManager.putString(Constants.KEY_PNAME, docSnapshot.getString(Constants.KEY_PNAME));
                                                preferenceManager.putString(Constants.KEY_IMAGE, docSnapshot.getString(Constants.KEY_IMAGE));
                                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            } else { // email google user absent => signup
                                                signUpGoogle();
                                            }
                                        });
                            } else {
                                showToast("La connexion a échoué");
                            }
                        }
                    });
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseApp.initializeApp(this);
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(SignInActivity.this, options);
        auth = FirebaseAuth.getInstance();

        setListeners();
    }

    private void setListeners() {
        binding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
        binding.googleSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }
        });
    }

    private void signIn() {
        loading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPasswd.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot docSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, docSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, docSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_PNAME, docSnapshot.getString(Constants.KEY_PNAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, docSnapshot.getString(Constants.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast("Impossible de se connecter!");
                    }
                });
    }

    private void encodeImage() {
        Uri imageUri = user.getPhotoUrl();
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            int previewWidth = 150;
            int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
            Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();

            encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void signUpGoogle() {
        String displayName = user.getDisplayName();
        assert displayName != null;
        String[] parts = displayName.split(" ");
        firstName = parts[0];
        lastName = parts.length > 1 ? parts[parts.length - 1] : "";
        encodeImage();

        FirebaseFirestore datab = FirebaseFirestore.getInstance();
        HashMap<String, Object> userH = new HashMap<>();
        userH.put(Constants.KEY_NAME, lastName);
        userH.put(Constants.KEY_PNAME, firstName);
        userH.put(Constants.KEY_EMAIL, user.getEmail());
        userH.put(Constants.KEY_GOOGLE_USER, 1);
        userH.put(Constants.KEY_IMAGE, encodedImage);
        datab.collection(Constants.KEY_COLLECTION_USERS)
                .add(userH)
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, lastName);
                    preferenceManager.putString(Constants.KEY_PNAME, firstName);
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exc -> {
                    showToast(exc.getMessage());
                });
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Entrez votre adresse mail");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Email invalide!");
            return false;
        } else if (binding.inputPasswd.getText().toString().trim().isEmpty()) {
            showToast("Entrez votre mot de passe");
            return false;
        } else {
            return true;
        }
    }


}