package com.example.smart_study;// Dans votre classe LoginActivity.java
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// ... autres imports
import com.example.smart_study.R; // Pour R.id.google_sign_in_button
import com.example.smart_study.services.GoogleAuthManager;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private GoogleAuthManager googleAuthManager;
    private Button googleSignInButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialiser le GoogleAuthManager
        googleAuthManager = new GoogleAuthManager(this);

        // 2. Récupérer le bouton depuis le layout
        googleSignInButton = findViewById(R.id.google_sign_in_button); // Assurez-vous que l'ID est correct

        // 3. Attribuer le listener au clic
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lancez le processus d'inscription/connexion
                // J'utilise signUp pour montrer la fenêtre de sélection de compte.
                // Vous pouvez appeler signInWithAuthorizedAccounts pour une connexion silencieuse d'abord.
                googleAuthManager.signUpWithGoogle(new GoogleAuthManager.SignInCallback() {
                    @Override
                    public void onSuccess(GoogleIdTokenCredential credential) {
                        // 4. Succès de l'authentification !
                        // C'est ici que vous envoyez le jeton (credential.getIdToken()) à votre backend
                        // ou que vous passez à l'activité suivante.
                        Log.d(TAG, "Connexion réussie ! Token ID: " + credential.getIdToken());
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Connexion réussie: " + credential.getDisplayName(), Toast.LENGTH_SHORT).show();
                            // Exemple: Intent vers MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // 5. Échec de l'authentification
                        // Affiche la trace complète de l'erreur dans la console Logcat pour un débogage détaillé.
                        Log.e(TAG, "La connexion Google a échoué. Cause :", exception);

                        runOnUiThread(() -> {
                            // Affiche un message d'erreur plus précis à l'utilisateur.
                            Toast.makeText(LoginActivity.this, "Échec: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });
    }
}
