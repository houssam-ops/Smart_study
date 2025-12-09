package com.example.smart_study.services;

import android.content.Context;
// À AJOUTER
import androidx.credentials.CredentialManager;

import android.util.Log;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.smart_study.AuthConstants;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;

import java.security.SecureRandom;
import android.util.Base64; // Assurez-vous d'avoir cet import
import java.security.SecureRandom;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleAuthManager {

    private static final String TAG = "GoogleAuthManager";
    private final Context context;
    private final CredentialManager credentialManager;
    private final Executor executor;

    public GoogleAuthManager(Context context) {
        this.context = context;
        this.credentialManager = CredentialManager.create(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Interface pour les callbacks de connexion
     */
    public interface SignInCallback {
        void onSuccess(GoogleIdTokenCredential credential);
        void onFailure(Exception exception);
    }

    /**
     * Génère un nonce sécurisé
     */
    private String generateNonce() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        // Utilisez android.util.Base64.URL_SAFE et NO_PADDING pour un résultat équivalent
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    /**
     * Connexion avec comptes autorisés (utilisateurs connus)
     */
    public void signInWithAuthorizedAccounts(SignInCallback callback) {
        String nonce = generateNonce();

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)  // Seulement comptes connus
                .setServerClientId(AuthConstants.WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)  // Connexion automatique
                .setNonce(nonce)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                context,
                request,
                null,  // CancellationSignal
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result, callback);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Erreur lors de la connexion", e);
                        callback.onFailure(e);
                    }
                }
        );
    }

    /**
     * Inscription de nouveaux utilisateurs (tous les comptes Google)
     */
    public void signUpWithGoogle(SignInCallback callback) {
        String nonce = generateNonce();

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)  // Tous les comptes
                .setServerClientId(AuthConstants.WEB_CLIENT_ID)
                .setNonce(nonce)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                context,
                request,
                null,
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result, callback);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Erreur lors de l'inscription", e);
                        callback.onFailure(e);
                    }
                }
        );
    }


    /**
     * Traite le résultat de l'authentification
     */
    private void handleSignInResult(GetCredentialResponse result, SignInCallback callback) {
        Credential credential = result.getCredential();

        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                // The createFrom method no longer throws GoogleIdTokenParsingException
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(customCredential.getData());

                // Perform a null check instead of catching the exception
                if (googleIdTokenCredential != null) {
                    // Succès !
                    callback.onSuccess(googleIdTokenCredential);
                } else {
                    // Handle the case where parsing fails and the credential is null
                    Exception error = new Exception("Jeton Google ID invalide ou impossible à analyser");
                    Log.e(TAG, error.getMessage());
                    callback.onFailure(error);
                }

            } else {
                Exception error = new Exception("Type d'identifiant inattendu: " + customCredential.getType());
                Log.e(TAG, error.getMessage());
                callback.onFailure(error);
            }
        } else {
            Exception error = new Exception("Type de credential non géré");
            Log.e(TAG, error.getMessage());
            callback.onFailure(error);
        }
    }

// ... (rest of the file remains the same) ...

    /**
     * Déconnexion : efface l'état des identifiants
     */
    public void signOut(SignOutCallback callback) {
        ClearCredentialStateRequest request = new ClearCredentialStateRequest();

        credentialManager.clearCredentialStateAsync(
                request,
                null,
                executor,
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        Log.d(TAG, "État des identifiants effacé");
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(ClearCredentialException e) {
                        Log.e(TAG, "Erreur lors de la déconnexion", e);
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                }
        );
    }

    /**
     * Interface pour les callbacks de déconnexion
     */
    public interface SignOutCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }
}