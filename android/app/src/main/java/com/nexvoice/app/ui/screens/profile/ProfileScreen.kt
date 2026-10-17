package com.nexvoice.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * ProfileScreen.kt — User Profile Interface (Image 2 style)
 *
 * Features:
 *  - Top bar with Close (✕), "Edit Profile" title, and Save (✓)
 *  - Scenic dark gradient banner cover with stylized watermark
 *  - Overlapping circular avatar with "Change photo" action
 *  - Editable Name, Username, Email, Bio fields
 *  - Gaming Audio toggles (Noise Suppression, Squad Low-Latency)
 *  - Styled Logout button with confirmation dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    userBio: String,
    onSaveProfile: (name: String, bio: String) -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var editName by remember { mutableStateOf(userName) }
    var editBio by remember { mutableStateOf(userBio) }
    var noiseSuppression by remember { mutableStateOf(true) }
    var lowLatencyMode by remember { mutableStateOf(true) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Full-screen overlay with status bar padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // ── TOP BANNER & ACTION BAR (Image 2 style) ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F2027),
                                Color(0xFF203A43),
                                Color(0xFF2C5364)
                            )
                        )
                    )
            ) {
                // Background ambient watermark
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLUTCH",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.08f),
                        letterSpacing = 8.sp
                    )
                }

                // Top Navigation Bar (Close '✕' on left, Title center, Save '✓' on right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            onSaveProfile(editName, editBio)
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                            onClose()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // ── OVERLAPPING AVATAR (Image 2 style) ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-50).dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Outer white circular border around avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .padding(4.dp)
                    ) {
                        // Inner avatar gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF2196F3),
                                            Color(0xFF1976D2),
                                            Color(0xFF0D47A1)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = editName.take(1).uppercase().ifEmpty { "C" },
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    // "Change photo" clickable label
                    Text(
                        text = "Change photo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2196F3),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                Toast.makeText(context, "Photo upload coming soon!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // ── PROFILE FIELDS (Image 2 style: Name, Username, Email, Bio) ─
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Display Name Field
                ProfileFieldItem(
                    label = "Name",
                    value = editName,
                    onValueChange = { editName = it },
                    placeholder = "Your display name"
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Username (Handle)
                ProfileFieldItem(
                    label = "Username",
                    value = "@${editName.lowercase().replace(" ", "_")}",
                    readOnly = true,
                    placeholder = "@username"
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Email
                ProfileFieldItem(
                    label = "Email",
                    value = userEmail.ifEmpty { "Not specified" },
                    readOnly = true,
                    placeholder = "user@example.com"
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Bio / Status
                ProfileFieldItem(
                    label = "Bio",
                    value = editBio,
                    onValueChange = { editBio = it },
                    placeholder = "Tell your squad about yourself"
                )

                Spacer(Modifier.height(12.dp))

                // ── GAMING AUDIO TOGGLES (Image 2 style switches) ──────────
                Text(
                    text = "VOICE PREFERENCES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                ProfileSwitchRow(
                    label = "Noise Suppression",
                    subtitle = "Filter background keyboard & fan noise",
                    checked = noiseSuppression,
                    onCheckedChange = { noiseSuppression = it }
                )

                ProfileSwitchRow(
                    label = "Squad Low-Latency Mode",
                    subtitle = "Sub-100ms ultra-low latency voice packet routing",
                    checked = lowLatencyMode,
                    onCheckedChange = { lowLatencyMode = it }
                )

                Spacer(Modifier.height(20.dp))

                // ── LOGOUT BUTTON ───────────────────────────────────────────
                Button(
                    onClick = { showLogoutConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log out",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }

                Text(
                    text = "CLUTCH • Low-Latency Squad Voice v1.0",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp)
                )
            }
        }
    }

    // ── LOGOUT CONFIRMATION DIALOG ───────────────────────────────────────────
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Text(
                    text = "Log out of CLUTCH?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "You will need to log back in to join voice rooms and chat with your squad.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── REUSABLE PROFILE FIELD ITEM ──────────────────────────────────────────────
@Composable
private fun ProfileFieldItem(
    label: String,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    readOnly: Boolean = false,
    placeholder: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF555555),
            modifier = Modifier.width(90.dp)
        )

        if (readOnly || onValueChange == null) {
            Text(
                text = value.ifEmpty { placeholder },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (value.isNotEmpty()) Color.Black else Color.Gray,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, fontSize = 13.sp, color = Color.LightGray) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    color = Color.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF9F9F9),
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
        }
    }
}

// ── PROFILE SWITCH ROW (Image 2 style) ───────────────────────────────────────
@Composable
private fun ProfileSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1B263B),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}
