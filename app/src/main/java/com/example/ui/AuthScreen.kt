package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.StreamRewardsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AppLogo() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(110.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(ElectricPink.copy(alpha = 0.3f), Color.Transparent)
                )
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .border(2.5.dp, Brush.linearGradient(listOf(ElectricPink, NeonCyan)), RoundedCornerShape(24.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ConnectedTv,
                    contentDescription = "Logo StreamRewards",
                    tint = NeonCyan,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .background(ElectricPink, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SR PRO",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: StreamRewardsViewModel) {
    val context = LocalContext.current

    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authSuccessLog by viewModel.authSuccessLog.collectAsStateWithLifecycle()

    var isSignUpTab by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var referralInput by remember { mutableStateOf("") }

    // --- PROTECTED SECURITY ACCESS BY PIN ---
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pendingEmail by remember { mutableStateOf("") }
    var pendingDisplayName by remember { mutableStateOf("") }
    var pendingIsSignUp by remember { mutableStateOf(false) }

    // --- REAL GOOGLE PLAY SERVICES SIGN IN ---
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: ""
                val displayName = account?.displayName ?: email.substringBefore("@")
                
                if (email.isNotBlank()) {
                    if (email.trim().equals("blandonjose6788@gmail.com", ignoreCase = true)) {
                        pendingEmail = email.trim()
                        pendingDisplayName = displayName
                        pendingIsSignUp = false
                        showAdminPinDialog = true
                        pinError = false
                        adminPinInput = ""
                    } else {
                        viewModel.loginWithGoogle(email, displayName)
                    }
                } else {
                    viewModel.setAuthError("No se pudo obtener el correo de la cuenta de Google.")
                }
            } catch (e: ApiException) {
                viewModel.setAuthError("Error Google Sign-In: ${e.message} (Código ${e.statusCode})")
            } catch (e: Exception) {
                viewModel.setAuthError("Error de Google: ${e.localizedMessage}")
            }
        } else {
            viewModel.setAuthError("Inicio de sesión con Google cancelado.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CinemaDarkReal, CardBackgroundPurple, CinemaDarkReal)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- LOGO & HEADER ---
            AppLogo()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "StreamRewards Pro",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = "Firebase Auth Hub",
                color = NeonCyan,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- TAB SWITCHER (Login / Register) ---
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Button(
                        onClick = { isSignUpTab = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSignUpTab) ElectricPink else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { isSignUpTab = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSignUpTab) ElectricPink else Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Registrarse", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- FORM CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackgroundPurple),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSignUpTab) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nombre Completo") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = SoftGray) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = NeonCyan,
                                unfocusedIndicatorColor = SoftGray,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = SoftGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = SoftGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = SoftGray,
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = SoftGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = SoftGray) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = SoftGray,
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = SoftGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isSignUpTab) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = referralInput,
                            onValueChange = { referralInput = it },
                            label = { Text("Código de Referido (Opcional)") },
                            leadingIcon = { Icon(Icons.Filled.Redeem, contentDescription = null, tint = SoftGray) },
                            placeholder = { Text("ej. REG-BONUS", color = SoftGray.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = NeonCyan,
                                unfocusedIndicatorColor = SoftGray,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = SoftGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (authLoading) {
                        CircularProgressIndicator(color = ElectricPink)
                    } else {
                        Button(
                            onClick = {
                                val trimmedEmail = emailInput.trim()
                                if (trimmedEmail.equals("blandonjose6788@gmail.com", ignoreCase = true)) {
                                    pendingEmail = trimmedEmail
                                    pendingDisplayName = nameInput.ifBlank { "Jose Blandón" }
                                    pendingIsSignUp = isSignUpTab
                                    showAdminPinDialog = true
                                    pinError = false
                                    adminPinInput = ""
                                } else {
                                    if (isSignUpTab) {
                                        val finalName = nameInput.ifBlank { emailInput.substringBefore("@") }
                                        viewModel.registerWithEmailAndPassword(
                                            email = emailInput,
                                            passwordDecoded = passwordInput,
                                            fullName = finalName,
                                            referralCode = referralInput
                                        )
                                    } else {
                                        viewModel.loginWithEmailAndPassword(emailInput, passwordInput)
                                    }
                                }
                            },
                            enabled = emailInput.isNotBlank() && passwordInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isSignUpTab) "CREAR CUENTA" else "ENTRAR CON CORREO",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Display authentication warnings / errors
                    authError?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = ElectricPink,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- OR ACCESS WITH GOOGLE ACTIONS ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = SoftGray.copy(alpha = 0.2f))
                Text(" O DISPOSITIVOS SOCIALES ", color = SoftGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.weight(1f), color = SoftGray.copy(alpha = 0.2f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google & Gmail Sign In Custom Button
            Button(
                onClick = { 
                    try {
                        googleSignInClient.signOut().addOnCompleteListener {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        }
                    } catch (e: Exception) {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mail,
                        contentDescription = "Google Logo",
                        tint = ElectricPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Iniciar Sesión con Google",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Firebase Status Badge
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Green, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = authSuccessLog ?: "Firebase listo",
                        color = SoftGray,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }

    // --- DIÁLOGO DE VALIDACIÓN DE PIN ADMINISTRADOR (MÁXIMA SEGURIDAD) ---
    if (showAdminPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminPinDialog = false
                adminPinInput = ""
                pinError = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Admin Shield",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🔒 Validación de Admin",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "El correo blandonjose6788@gmail.com pertenece al Administrador Principal. Para verificar el acceso legítimo en este móvil, por favor ingresa su PIN de Seguridad Administrativa:",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = { 
                            if (it.length <= 6) {
                                adminPinInput = it
                                pinError = false
                            }
                        },
                        label = { Text("PIN de Acceso Seguro") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = SoftGray) },
                        singleLine = true,
                        placeholder = { Text("Ingresa el PIN", color = SoftGray.copy(alpha = 0.5f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = SoftGray,
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = SoftGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (pinError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PIN incorrecto. Acceso restringido por seguridad.",
                            color = ElectricPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // El PIN de Acceso Seguro es el 6788 (últimos 4 dígitos correspondientes a su correo blandonjose6788)
                        if (adminPinInput == "6788") {
                            showAdminPinDialog = false
                            adminPinInput = ""
                            pinError = false
                            if (pendingEmail.isNotBlank()) {
                                if (pendingIsSignUp) {
                                    viewModel.registerWithEmailAndPassword(
                                        email = pendingEmail,
                                        passwordDecoded = passwordInput,
                                        fullName = pendingDisplayName,
                                        referralCode = referralInput
                                    )
                                } else {
                                    viewModel.loginWithGoogle(pendingEmail, pendingDisplayName)
                                }
                            }
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirmar", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdminPinDialog = false
                        adminPinInput = ""
                        pinError = false
                    }
                ) {
                    Text("Cancelar", color = SoftGray)
                }
            },
            containerColor = CardBackgroundPurple
        )
    }
}
