package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.AuthRepository
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceWhite
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SignInScreen"

@Composable
fun SignInScreen(
    authRepository: AuthRepository,
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Se connecter, 1: Créer un compte

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Forgot password dialog
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sign_in_screen"),
        color = NabdBone
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_in_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NabdPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sécurité de l'atelier",
                            modifier = Modifier.size(32.dp),
                            tint = NabdPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Suivi",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Gestion d'Atelier & Pointage d'Équipe",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = NabdDeepViolet,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tab Selector: Se connecter vs Créer un compte
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = NabdBone,
                        contentColor = NabdPurple,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = NabdPurple,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                                infoMessage = null
                            },
                            text = {
                                Text(
                                    text = "Se connecter",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_sign_in")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                                infoMessage = null
                            },
                            text = {
                                Text(
                                    text = "Créer un compte",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_sign_up")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            color = Color(0xFFFBEBEB),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFC0392B),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (infoMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = infoMessage ?: "",
                                color = Color(0xFF2E7D32),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (selectedTab == 1) {
                        // Registration: Nom complet
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nom et prénom") },
                            placeholder = { Text("ex: Fatma Trabelsi") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "Nom", tint = NabdDeepViolet)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NabdPurple,
                                unfocusedBorderColor = NabdBorder
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Email input
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Adresse email") },
                        placeholder = { Text("ex: direction@atelier.tn") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = NabdDeepViolet)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(if (selectedTab == 0) "Mot de passe" else "Mot de passe (6 car. min.)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Mot de passe", tint = NabdDeepViolet)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Masquer" else "Afficher",
                                    tint = NabdDeepViolet
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder
                        )
                    )

                    if (selectedTab == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Mot de passe oublié ?",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NabdPurple,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .clickable {
                                        resetEmailInput = emailInput.trim()
                                        showForgotPasswordDialog = true
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("forgot_password_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Action Button
                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            val email = emailInput.trim()
                            val password = passwordInput.trim()

                            if (email.isEmpty() || !email.contains("@")) {
                                errorMessage = "Veuillez saisir une adresse email valide."
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "Le mot de passe doit contenir au moins 6 caractères."
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null
                            infoMessage = null

                            coroutineScope.launch {
                                if (selectedTab == 0) {
                                    // Se connecter
                                    val res = authRepository.signInWithEmail(email, password)
                                    isLoading = false
                                    if (res.isSuccess) {
                                        onSignInSuccess()
                                    } else {
                                        errorMessage = "Erreur de connexion : identifiants incorrects ou compte inexistant."
                                    }
                                } else {
                                    // Créer un compte
                                    val name = nameInput.trim()
                                    if (name.length < 2) {
                                        isLoading = false
                                        errorMessage = "Veuillez renseigner votre nom complet."
                                        return@launch
                                    }
                                    val res = authRepository.signUpWithEmail(email, password, name)
                                    isLoading = false
                                    if (res.isSuccess) {
                                        onSignInSuccess()
                                    } else {
                                        errorMessage = "Erreur de création : ${res.exceptionOrNull()?.localizedMessage ?: "Vérifiez vos données."}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag(if (selectedTab == 0) "submit_sign_in_button" else "submit_sign_up_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NabdPurple,
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (selectedTab == 0) "Se connecter" else "Créer mon compte",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = NabdBorder)
                        Text(
                            text = "  ou  ",
                            style = MaterialTheme.typography.bodySmall.copy(color = NabdMineralGray)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = NabdBorder)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            if (isLoading) return@OutlinedButton
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                performGoogleSignIn(
                                    context = context,
                                    onSuccess = {
                                        val cur = FirebaseAuth.getInstance().currentUser
                                        if (cur != null) {
                                            coroutineScope.launch {
                                                val syncResult = runCatching {
                                                    authRepository.syncUserAccountAndMemberships(cur)
                                                }
                                                isLoading = false
                                                if (syncResult.isSuccess) {
                                                    onSignInSuccess()
                                                } else {
                                                    FirebaseAuth.getInstance().signOut()
                                                    errorMessage = "Google a réussi l'authentification, mais Firebase n'est pas correctement configuré pour Suivi."
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            onSignInSuccess()
                                        }
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    },
                                    onCancelled = {
                                        isLoading = false
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_in_with_google_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NabdObsidian)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Google Sign-In",
                                modifier = Modifier.size(18.dp),
                                tint = NabdPurple
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedTab == 0) "Continuer avec Google" else "S'inscrire avec Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Authentification chiffrée Firebase • Isolation absolue par entreprise",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NabdMineralGray,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingReset) showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "Réinitialiser le mot de passe",
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
            },
            text = {
                Column {
                    Text(
                        text = "Saisissez votre adresse email pour recevoir un lien sécurisé de réinitialisation :",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdDeepViolet
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("Adresse email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = resetEmailInput.trim()
                        if (email.isEmpty() || !email.contains("@")) {
                            errorMessage = "Veuillez entrer une adresse valide."
                            return@Button
                        }
                        isSendingReset = true
                        coroutineScope.launch {
                            val res = authRepository.sendPasswordResetEmail(email)
                            isSendingReset = false
                            showForgotPasswordDialog = false
                            if (res.isSuccess) {
                                infoMessage = "Un email de réinitialisation a été envoyé à $email."
                            } else {
                                errorMessage = "Impossible d'envoyer l'email de réinitialisation."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                    enabled = !isSendingReset
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Envoyer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }, enabled = !isSendingReset) {
                    Text("Annuler")
                }
            }
        )
    }
}

private suspend fun performGoogleSignIn(
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    try {
        val credentialManager = CredentialManager.create(context)
        val webClientId = context.getString(R.string.default_web_client_id)
        if (webClientId.contains("placeholder", ignoreCase = true) ||
            !webClientId.endsWith(".apps.googleusercontent.com")) {
            onError("Google n'est pas encore configuré : ajoutez le vrai google-services.json de votre projet Firebase.")
            return
        }

        val googleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val response = credentialManager.getCredential(context = context, request = request)
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()

            if (authResult.user != null) {
                onSuccess()
            } else {
                onError("Connexion impossible : utilisateur introuvable")
            }
        } else {
            onError("Type d'authentification inattendu")
        }
    } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled by user", e)
        onCancelled()
    } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onError("Erreur d'authentification : ${e.localizedMessage ?: "Veuillez réessayer"}")
    }
}
