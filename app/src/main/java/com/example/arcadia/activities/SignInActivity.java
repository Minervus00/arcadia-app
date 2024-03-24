package com.example.arcadia.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.arcadia.databinding.ActivitySignInBinding;
import com.example.arcadia.utilities.Constants;
import com.example.arcadia.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
//    FirebaseAuth auth;
//    FirebaseUser user;
//    GoogleSignInClient googleSignInClient;
//    String lastName, firstName, encodedImage;

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

//    private final ActivityResultLauncher <Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
//        @Override
//        public void onActivityResult(ActivityResult res) {
////            showToast("In result" + res.getResultCode());
//            if (res.getResultCode() == Activity.RESULT_OK) {
//                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(res.getData());
//                try {
//                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
//                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
//                    auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                        @Override
//                        public void onComplete(@NonNull Task<AuthResult> task) {
//                            if (task.isSuccessful()) {
//                                user = FirebaseAuth.getInstance().getCurrentUser();
//                                // Vérifier si email + passwd existe si oui signInGoogle
//                                // Sinon SignUpGoogle
//                                FirebaseFirestore db = FirebaseFirestore.getInstance();
//                                db.collection(Constants.KEY_COLLECTION_USERS)
//                                        .whereEqualTo(Constants.KEY_EMAIL, user.getEmail())
//                                        .whereEqualTo(Constants.KEY_GOOGLE_USER, 1)
//                                        .get()
//                                        .addOnCompleteListener(task2 -> {
////                                            Log.i("TAB1", "Oncomp");
//                                            if (task2.isSuccessful() && task2.getResult() != null
//                                                    && task2.getResult().getDocuments().size() > 0) { // email google user existe => signin
////                                                Log.i("TAB1", "On going!");
//                                                DocumentSnapshot docSnapshot = task2.getResult().getDocuments().get(0);
//                                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
//                                                preferenceManager.putString(Constants.KEY_USER_ID, docSnapshot.getId());
//                                                preferenceManager.putString(Constants.KEY_NAME, docSnapshot.getString(Constants.KEY_NAME));
//                                                preferenceManager.putString(Constants.KEY_PNAME, docSnapshot.getString(Constants.KEY_PNAME));
//                                                preferenceManager.putString(Constants.KEY_IMAGE, docSnapshot.getString(Constants.KEY_IMAGE));
//                                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                                startActivity(intent);
//                                            } else { // email google user absent => signup
//                                                Log.i("TAB1", "Nouveau google user!");
//                                                signUpGoogle();
//                                            }
//                                        })
//                                        .addOnFailureListener(exc -> {
//                                            showToast(exc.getMessage());
//                                        });
//                            } else {
//                                showToast("La connexion a échoué");
//                            }
//                        }
//                    });
//                } catch (ApiException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    });

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

//        FirebaseApp.initializeApp(this);
//        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.client_id))
//                .requestEmail()
//                .build();
//        googleSignInClient = GoogleSignIn.getClient(SignInActivity.this, options);
//        auth = FirebaseAuth.getInstance();

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
//        binding.googleSigninButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = googleSignInClient.getSignInIntent();
//                activityResultLauncher.launch(intent);
//            }
//        });
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

//    private void encodeImage() {
//        String imageUrl = Objects.requireNonNull(user.getPhotoUrl()).toString();
//        final Bitmap[] bitmap = new Bitmap[1];
//        Glide.with(this)
//                .asBitmap()
//                .load(imageUrl)
//                .into(new CustomTarget<Bitmap>() {
//                    @Override
//                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
//                        // Encode Bitmap to String
//                        bitmap[0] = resource;
//                        int previewWidth = 150;
//                        int previewHeight = bitmap[0].getHeight() * previewWidth / bitmap[0].getWidth();
//                        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap[0], previewWidth, previewHeight, false);
//                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
//                        byte[] bytes = byteArrayOutputStream.toByteArray();
//                        Log.i("TAB1", Base64.encodeToString(bytes, Base64.DEFAULT));
//
//                        // Now you can use the encodedImage string as needed
//                        // For example, you can pass it to your function or store it in SharedPreferences
//                    }
//
//                    @Override
//                    public void onLoadCleared(@Nullable Drawable placeholder) {
//                        // This method is called when the image is not available (cleared)
//                    }
//                });

//        if (bitmap[0] != null) {
//            Log.i("TAB1", "encoding...");
//            int previewWidth = 150;
//            int previewHeight = bitmap[0].getHeight() * previewWidth / bitmap[0].getWidth();
//            Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap[0], previewWidth, previewHeight, false);
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
//            byte[] bytes = byteArrayOutputStream.toByteArray();
//            return Base64.encodeToString(bytes, Base64.DEFAULT);
//        } else {
//            Log.i("TAB1", "bitmap null");
//            return null;
//        }
//    }

//    private void signUpGoogle() {
//        Log.i("TAB1", "Entering signUpGoogle function");
//        String displayName = user.getDisplayName();
//        assert displayName != null;
//        String[] parts = displayName.split(" ");
//        firstName = parts[0];
//        lastName = parts.length > 1 ? parts[parts.length - 1] : "";
//        Log.i("TAB1", "Encoding started");
//        encodeImage();
//        Log.i("TAB1", "Encoding finished");
////        assert encodedImage != null;
//
//        FirebaseFirestore datab = FirebaseFirestore.getInstance();
//        HashMap<String, Object> userH = new HashMap<>();
//        userH.put(Constants.KEY_NAME, lastName);
//        userH.put(Constants.KEY_PNAME, firstName);
//        userH.put(Constants.KEY_EMAIL, user.getEmail());
//        userH.put(Constants.KEY_GOOGLE_USER, 1);
//        userH.put(Constants.KEY_IMAGE, encodedImage);
//        datab.collection(Constants.KEY_COLLECTION_USERS)
//                .add(userH)
//                .addOnSuccessListener(documentReference -> {
//                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
//                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
//                    preferenceManager.putString(Constants.KEY_NAME, lastName);
//                    preferenceManager.putString(Constants.KEY_PNAME, firstName);
//                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
//                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                })
//                .addOnFailureListener(exc -> {
//                    showToast(exc.getMessage());
//                });
//    }

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