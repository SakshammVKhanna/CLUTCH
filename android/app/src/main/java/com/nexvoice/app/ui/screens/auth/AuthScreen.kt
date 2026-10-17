package com.nexvoice.app.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexvoice.app.ui.theme.*

/**
 * AuthScreen.kt (Phase 3)
 *
 * Now connected to the real backend via AuthViewModel.
 * - Shows a loading spinner while the API call is in progress
 * - Navigates to Home on success
 * - Displays the real error message from the backend on failure
 */
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isLoginMode    by remember { mutableStateOf(true) }
    var username       by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Observe ViewModel state
    val uiState by viewModel.uiState.collectAsState()

    // When signup/login succeeds, navigate to Home
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }

    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexBg)
    ) {
        // Decorative gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(NexGold.copy(alpha = 0.08f), NexBg)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = null,
                tint = NexGold,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NexVoice",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NexGold
            )
            Text(
                text = if (isLoginMode) "Welcome back, Gamer!" else "Join the Squad",
                fontSize = 14.sp,
                color = NexTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // ── Tab Toggle ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexSurface)
                    .border(1.dp, NexBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                AuthTab("Login",   isLoginMode,  Modifier.weight(1f)) {
                    isLoginMode = true
                    viewModel.resetState()
                }
                AuthTab("Sign Up", !isLoginMode, Modifier.weight(1f)) {
                    isLoginMode = false
                    viewModel.resetState()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Form Card ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexSurface)
                    .border(1.dp, NexBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(visible = !isLoginMode) {
                    NexTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        leadingIcon = Icons.Default.Person,
                        enabled = !isLoading
                    )
                }

                NexTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    enabled = !isLoading
                )

                NexTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Password,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = NexTextSecondary
                            )
                        }
                    }
                )

                // Error message from backend
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = NexMuted,
                        fontSize = 13.sp
                    )
                }

                // ── Submit Button ──────────────────────────────────────────
                Button(
                    onClick = {
                        // Basic local validation before hitting the network
                        if (email.isBlank() || password.isBlank()) return@Button
                        if (!isLoginMode && username.isBlank()) return@Button

                        if (isLoginMode) {
                            viewModel.login(email.trim(), password)
                        } else {
                            viewModel.signup(username.trim(), email.trim(), password)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexGold,
                        contentColor = NexBg
                    )
                ) {
                    if (isLoading) {
                        // Spinner while API call is in progress
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NexBg,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isLoginMode) "Enter the Arena" else "Create Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AuthTab(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NexGold else NexSurface)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (isSelected) NexBg else NexTextSecondary
        )
    }
}

@Composable
private fun NexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = NexTextSecondary) },
        leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null, tint = NexGold) },
        trailingIcon = trailingIcon,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = NexGold,
            unfocusedBorderColor = NexBorder,
            focusedTextColor     = NexTextPrimary,
            unfocusedTextColor   = NexTextPrimary,
            cursorColor          = NexGold,
            focusedContainerColor   = NexSurfaceAlt,
            unfocusedContainerColor = NexSurfaceAlt
        )
    )
}
