package com.nexvoice.app.ui.screens.voiceroom

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.nexvoice.app.models.RoomMember
import com.nexvoice.app.realtime.LiveKitManager
import com.nexvoice.app.realtime.SocketManager
import com.nexvoice.app.ui.components.OfflineBanner
import com.nexvoice.app.utils.InviteLink
import com.nexvoice.app.ui.theme.*

/**
 * VoiceRoomScreen.kt — Phase 3 UI Redesign
 *
 * Premium Parallel-inspired voice room UI featuring:
 *  - Top header with back arrow, room title, game tag, settings + invite icons
 *  - Host "Owner" card with golden crown and speaking indicator
 *  - 3-column member avatar grid with:
 *      • Green pulsing ring = speaking
 *      • Red mic icon badge = muted
 *      • "+Invite" placeholder card
 *  - 5-button voice control dock (Mute, Deafen, Leave, Speaker, More)
 *  - Room Info bottom sheet (members, host, time)
 *
 * Phase 5: WebRTC replaces fake members with real voice data.
 */
@Composable
fun VoiceRoomScreen(
    roomId: String,
    onLeaveRoom: () -> Unit,
    viewModel: VoiceRoomViewModel = viewModel()
) {
    val context = LocalContext.current
    val hasMicPermission by viewModel.hasMicPermission.collectAsState()
    val permissionDeniedNotice by viewModel.permissionDeniedNotice.collectAsState()

    // ActivityResult launcher for requesting RECORD_AUDIO permission
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicrophonePermissionGranted()
        } else {
            viewModel.onMicrophonePermissionDenied()
        }
    }

    // Join real-time presence & request microphone permission when screen opens
    LaunchedEffect(roomId) {
        viewModel.enterRoom(roomId)

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            viewModel.onMicrophonePermissionGranted()
        } else {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Collect real-time state from ViewModel
    val liveMembers by viewModel.members.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isDeafened by viewModel.isDeafened.collectAsState()
    val isSpeaker by viewModel.isSpeaker.collectAsState()
    val roomInfo by viewModel.roomInfo.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val speakingIds by viewModel.speakingIds.collectAsState()

    val currentUserName by viewModel.currentUserName.collectAsState()

    val leftRoom by viewModel.leftRoom.collectAsState()
    LaunchedEffect(leftRoom) {
        if (leftRoom) {
            viewModel.resetLeft()
            onLeaveRoom()
        }
    }

    // Direct in-app invite state
    var showInviteDialog by remember { mutableStateOf(false) }
    // Room options bottom sheet (Settings / More buttons)
    var showRoomOptionsSheet by remember { mutableStateOf(false) }
    val isInviting by viewModel.isInviting.collectAsState()
    val inviteSuccess by viewModel.inviteSuccess.collectAsState()
    val inviteError by viewModel.inviteError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(inviteSuccess) {
        inviteSuccess?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInviteStatus()
            showInviteDialog = false
        }
    }

    // Determine host/owner from live room data (with LiveKit active-speaker state)
    val hostName = roomInfo?.hostName
    val owner = remember(liveMembers, hostName, speakingIds) {
        val base = if (liveMembers.isEmpty()) {
            RoomMember("connecting", hostName ?: "Connecting...", isMuted = false, isSpeaking = false)
        } else {
            liveMembers.find { hostName != null && it.username.equals(hostName, ignoreCase = true) }
                ?: liveMembers.first()
        }
        base.copy(isSpeaking = speakingIds.contains(base.id))
    }

    // Non-owner members rendered in the grid (with LiveKit active-speaker state)
    val nonOwnerMembers = remember(liveMembers, owner, speakingIds) {
        liveMembers.filter { it.id != owner.id }.map { member ->
            member.copy(isSpeaking = speakingIds.contains(member.id))
        }
    }

    // Room options bottom sheet — opened by Settings or More button
    if (showRoomOptionsSheet) {
        RoomOptionsSheet(
            roomId = roomId,
            roomName = roomInfo?.name ?: "Voice Room",
            onDismiss = { showRoomOptionsSheet = false },
            onInvite = {
                showRoomOptionsSheet = false
                showInviteDialog = true
            },
            onLeave = {
                showRoomOptionsSheet = false
                viewModel.leaveRoom(roomId)
            }
        )
    }

    if (showInviteDialog) {
        AddMemberDialog(
            roomId = roomId,
            roomName = roomInfo?.name ?: "Voice Room",
            isInviting = isInviting,
            inviteError = inviteError,
            onDismiss = {
                showInviteDialog = false
                viewModel.clearInviteStatus()
            },
            onAddUser = { target ->
                viewModel.inviteUser(target)
            },
            onShareLink = {
                shareInviteLink(
                    context = context,
                    roomName = roomInfo?.name ?: "Voice Room",
                    roomId = roomId
                )
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Changed from NexBg to white
    ) {
        // Light ambient glow (subtle)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.03f), // Light green glow
                            Color.Transparent
                        ),
                        center = Offset(Float.POSITIVE_INFINITY / 2, 0f),
                        radius = 600f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LightVoiceRoomTopBar( // Using light themed top bar
                    roomName = roomInfo?.name ?: "Voice Room",
                    gameTag  = roomInfo?.gameName ?: "Gaming", 
                    roomId   = roomId,
                    connectionState = connectionState,
                    onBack   = { viewModel.leaveRoom(roomId) },
                    onInvite = { showInviteDialog = true },
                    onSettings = { showRoomOptionsSheet = true }
                )
            },
            bottomBar = {
                LightVoiceControlDock( // Using light themed controls
                    isMuted    = isMuted,
                    isDeafened = isDeafened,
                    isSpeaker  = isSpeaker,
                    onToggleMute    = {
                        if (!hasMicPermission) {
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleMute(roomId)
                        }
                    },
                    onToggleDeafen  = { viewModel.toggleDeafen(roomId) },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onLeave         = { viewModel.leaveRoom(roomId) },
                    onMore          = { showRoomOptionsSheet = true }
                )
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(2.dp))

                // ── Offline Banner (shows when Socket.IO disconnected) ────────
                OfflineBanner(
                    isVisible = connectionState == SocketManager.ConnectionState.DISCONNECTED ||
                                connectionState == SocketManager.ConnectionState.ERROR,
                    onRetry = { viewModel.enterRoom(roomId) },
                    message = "Connection lost. Reconnecting…",
                    buttonText = "Retry Now"
                )

                // ── Microphone Permission Warning (if denied) ──────────────
                if (permissionDeniedNotice) {
                    MicrophonePermissionBanner(
                        onGrantClick = {
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onDismiss = {
                            viewModel.dismissPermissionNotice()
                        }
                    )
                }

                // ── Voice Connection Status & Host Card ────────────────────
                LightVoiceStatusRow(voiceState = voiceState)
                OwnerCard(owner = owner, isMe = owner.username.equals(currentUserName, ignoreCase = true))

                // ── Member Area: Solo Lobby vs Active Squad Grid ──────────
                if (nonOwnerMembers.isEmpty()) {
                    LightSoloLobbyCard(
                        roomId = roomId,
                        onAddMember = { showInviteDialog = true },
                        onShareLink = {
                            shareInviteLink(
                                context = context,
                                roomName = roomInfo?.name ?: "Voice Room",
                                roomId = roomId
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(nonOwnerMembers, key = { it.id }) { member ->
                            MemberTile(
                                member = member,
                                isMe = member.username.equals(currentUserName, ignoreCase = true)
                            )
                        }
                        item(key = "invite_tile") {
                            InviteTile(onClick = { showInviteDialog = true })
                        }
                    }
                }

                // ── Room Info Card (bottom info strip) ─────────────────────
                RoomInfoCard(
                    gameName    = roomInfo?.gameName ?: "Gaming",
                    memberCount = liveMembers.size
                )

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── LIGHT THEMED TOP BAR ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LightVoiceRoomTopBar(
    roomName: String,
    gameTag: String,
    roomId: String = "",
    connectionState: SocketManager.ConnectionState = SocketManager.ConnectionState.CONNECTED,
    onBack: () -> Unit,
    onInvite: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    TopAppBar(
        modifier = Modifier
            .drawBehind {
                drawLine(
                    color = Color(0xFFE0E0E0),
                    start = Offset(0f, size.height),
                    end   = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    "Back",
                    tint = Color.Black
                )
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            gameTag,
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        roomName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (roomId.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                                clip?.setPrimaryClip(android.content.ClipData.newPlainText("Room ID", roomId))
                                Toast.makeText(context, "Room Code copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(top = 2.dp, end = 4.dp)
                    ) {
                        Text(
                            "Code: ${roomId.take(8)}...",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        },
        actions = {
            // Settings icon
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    "Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Invite icon
            IconButton(onClick = onInvite) {
                Icon(
                    Icons.Default.PersonAdd,
                    "Invite Squad",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

// ── LIGHT THEMED VOICE STATUS ROW ─────────────────────────────────────────────
@Composable
private fun LightVoiceStatusRow(voiceState: LiveKitManager.VoiceState) {
    val (dotColor, label) = when (voiceState) {
        LiveKitManager.VoiceState.CONNECTED -> Color(0xFF4CAF50) to "Voice connected"
        LiveKitManager.VoiceState.CONNECTING -> Color(0xFFFF9800) to "Connecting voice…"
        LiveKitManager.VoiceState.RECONNECTING -> Color(0xFFFF9800) to "Reconnecting voice…"
        LiveKitManager.VoiceState.FAILED -> Color(0xFFFF9800) to "Presence active • Voice connecting…"
        LiveKitManager.VoiceState.DISCONNECTED -> Color.Gray to "Voice idle"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}

// ── LIGHT THEMED SOLO LOBBY CARD ─────────────────────────────────────────────
@Composable
private fun LightSoloLobbyCard(
    roomId: String,
    onAddMember: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "lobby_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Radar pulse icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape)
                    .border(1.5.dp, Color(0xFF4CAF50).copy(alpha = pulseAlpha * 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.HeadsetMic,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Lobby is Ready",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "You're alone right now. Add friends by username or share the Room Code to start talking.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            // Room ID chip with 1-tap copy
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                    .clickable {
                        val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                        clip?.setPrimaryClip(android.content.ClipData.newPlainText("Room Code", roomId))
                        Toast.makeText(context, "Room Code copied!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ROOM CODE: $roomId",
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(13.dp)
                )
            }

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddMember,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Add Friend by Username",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onShareLink,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Share Invite Link",
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ── DIRECT IN-APP ADD MEMBER DIALOG ──────────────────────────────────────────
@Composable
private fun AddMemberDialog(
    roomId: String,
    roomName: String,
    isInviting: Boolean,
    inviteError: String?,
    onDismiss: () -> Unit,
    onAddUser: (String) -> Unit,
    onShareLink: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.GroupAdd, null, tint = NexGold, modifier = Modifier.size(22.dp))
                Text(
                    "Add Member to Room",
                    color = NexTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Enter their username or email to add them directly into this room.",
                    fontSize = 12.sp,
                    color = NexTextSecondary
                )

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    placeholder = { Text("e.g. sakshamm or pro@gmail.com", color = NexTextHint, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NexTextPrimary,
                        unfocusedTextColor = NexTextPrimary,
                        focusedBorderColor = NexGold,
                        unfocusedBorderColor = NexBorder,
                        focusedContainerColor = NexSurfaceAlt,
                        unfocusedContainerColor = NexSurfaceAlt
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!inviteError.isNullOrBlank()) {
                    Text(
                        inviteError,
                        color = NexMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { onAddUser(usernameInput) },
                    enabled = usernameInput.isNotBlank() && !isInviting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexGold,
                        contentColor = NexOnAccent,
                        disabledContainerColor = NexGold.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    if (isInviting) {
                        CircularProgressIndicator(color = NexOnAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Add, null, tint = NexOnAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Room", color = NexOnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(color = NexBorder, thickness = 1.dp)

                // Copy Room ID option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexSurfaceAlt)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text("Room Code", fontSize = 10.sp, color = NexTextSecondary)
                        Text(
                            roomId,
                            fontSize = 11.sp,
                            color = NexTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = {
                            val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                            clip?.setPrimaryClip(android.content.ClipData.newPlainText("Room Code", roomId))
                            Toast.makeText(context, "Room Code copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Copy", color = NexGoldDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Share link
                OutlinedButton(
                    onClick = onShareLink,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexTextPrimary),
                    border = BorderStroke(1.dp, NexBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Icon(Icons.Default.Share, null, tint = NexTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Invite Link", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NexTextSecondary)
            }
        }
    )
}

// ── INVITE SHARE ───────────────────────────────────────────────────────────────
// YouTube-style: copies the invite link AND opens the system share sheet,
// so friends get it via WhatsApp / copy-paste — both work with Join via Link.
private fun shareInviteLink(context: android.content.Context, roomName: String, roomId: String) {
    val link = InviteLink.build(roomId)

    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("NexVoice invite", link))
    Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show()

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, InviteLink.shareMessage(roomName, roomId))
    }
    context.startActivity(Intent.createChooser(send, "Invite via…"))
}

// ── OWNER CARD ────────────────────────────────────────────────────────────────
@Composable
private fun OwnerCard(owner: RoomMember, isMe: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "owner_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue  = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "owner_glow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        NexGold.copy(alpha = 0.10f),
                        Color.White
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(NexGold.copy(alpha = glowAlpha), NexBorder)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar with golden ring
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
            // Glowing ring (turns green when speaking)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, if (owner.isSpeaking) NexSpeaking else NexGold, CircleShape)
                    .background(
                        if (owner.isSpeaking) NexSpeaking.copy(alpha = 0.15f) else NexGold.copy(alpha = 0.08f),
                        CircleShape
                    )
            )
            // Avatar initial
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NexGold.copy(alpha = 0.28f), Color.White)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    owner.username.take(1).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NexGoldDark
                )
            }
            // Mic status badge (bottom-right)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .background(if (owner.isMuted) NexMuted else NexSpeaking, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (owner.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Name + crown + label
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    owner.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NexTextPrimary
                )
                // Crown emoji
                Text("👑", fontSize = 14.sp)
                if (isMe) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NexGold.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("You", fontSize = 10.sp, color = NexGoldDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Status dot + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(if (owner.isMuted) NexMuted else NexOnline, CircleShape)
                )
                Text(
                    if (owner.isMuted) "Muted" else "Host",
                    fontSize = 12.sp,
                    color = NexTextSecondary
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // "Host" badge on right
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NexGold.copy(alpha = 0.12f))
                .border(1.dp, NexGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                "HOST",
                fontSize = 10.sp,
                color = NexGoldDark,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── MEMBER TILE ───────────────────────────────────────────────────────────────
@Composable
private fun MemberTile(member: RoomMember, isMe: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "member_pulse_${member.id}")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_${member.id}"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_${member.id}"
    )

    // Ring color based on state
    val ringColor = when {
        member.isSpeaking -> NexSpeaking
        member.isMuted    -> NexMuted
        else              -> NexBorder
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                width = if (isMe) 2.dp else 1.dp,
                color = when {
                    isMe -> NexGold.copy(alpha = 0.8f)
                    member.isSpeaking -> NexSpeaking.copy(alpha = 0.5f)
                    else -> NexBorder
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        // Avatar with speaking ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .scale(if (member.isSpeaking) pulseScale else 1f)
        ) {
            // Speaking glow ring
            if (member.isSpeaking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            NexSpeaking.copy(alpha = glowAlpha * 0.15f),
                            CircleShape
                        )
                        .border(2.5.dp, NexSpeaking.copy(alpha = glowAlpha), CircleShape)
                )
            }

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(if (member.isSpeaking) 46.dp else 50.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NexGold.copy(alpha = 0.18f),
                                Color(0xFFF1F3F5)
                            )
                        ),
                        CircleShape
                    )
                    .border(
                        width = if (member.isSpeaking) 0.dp else 1.dp,
                        color = NexBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.username.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NexGoldDark
                )
            }

            // Mic status badge — bottom-right corner
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .background(
                        if (member.isMuted) NexMuted else NexOnline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (member.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Username
        Text(
            text = if (isMe) "${member.username} (You)" else member.username,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isMe) NexGoldDark else if (member.isSpeaking) NexTextPrimary else NexTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── INVITE TILE ───────────────────────────────────────────────────────────────
@Composable
private fun InviteTile(onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, NexBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        // + circle
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFFF1F3F5), CircleShape)
                .border(1.5.dp, NexBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                "Invite",
                tint = NexTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            "Invite",
            fontSize = 11.sp,
            color = NexTextSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// ── ROOM INFO CARD ────────────────────────────────────────────────────────────
@Composable
private fun RoomInfoCard(
    gameName: String,
    memberCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, NexBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Room Info",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = NexTextPrimary
        )

        // Game chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NexGold.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(gameName, fontSize = 10.sp, color = NexGoldDark, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.weight(1f))

        // Member count
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.People, null, tint = NexTextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("$memberCount members", fontSize = 11.sp, color = NexTextSecondary)
        }
    }
}

// ── LIGHT THEMED VOICE CONTROL DOCK ──────────────────────────────────────────
@Composable
private fun LightVoiceControlDock(
    isMuted: Boolean,
    isDeafened: Boolean,
    isSpeaker: Boolean,
    onToggleMute: () -> Unit,
    onToggleDeafen: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onLeave: () -> Unit,
    onMore: () -> Unit = {}
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Mute ──────────────────────────────────────────────────────
            LightVoiceControlButton(
                icon      = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label     = if (isMuted) "Unmute" else "Mute",
                isActive  = isMuted,
                activeColor = Color(0xFFE57373),
                onClick   = onToggleMute
            )

            // ── Deafen ────────────────────────────────────────────────────
            LightVoiceControlButton(
                icon      = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
                label     = "Deafen",
                isActive  = isDeafened,
                activeColor = Color(0xFFFF9800),
                onClick   = onToggleDeafen
            )

            // ── LEAVE (center — big red button) ───────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFE57373), CircleShape)
                        .clickable { onLeave() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        "Leave Room",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    "Leave",
                    fontSize = 12.sp,
                    color = Color(0xFFE57373),
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Speaker ───────────────────────────────────────────────────
            LightVoiceControlButton(
                icon      = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                label     = "Speaker",
                isActive  = !isSpeaker,  // active = muted speaker
                activeColor = Color(0xFFE57373),
                onClick   = onToggleSpeaker
            )

            // ── More ──────────────────────────────────────────────────────
            LightVoiceControlButton(
                icon      = Icons.Default.MoreHoriz,
                label     = "More",
                isActive  = false,
                activeColor = Color(0xFF4CAF50),
                onClick   = onMore
            )
        }
    }
}

// ── LIGHT THEMED VOICE CONTROL BUTTON ────────────────────────────────────────
@Composable
private fun LightVoiceControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    size: Dp = 52.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.15f) else Color(0xFFF5F5F5),
                    CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isActive) activeColor.copy(alpha = 0.6f) else Color(0xFFE0E0E0),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            label,
            fontSize = 11.sp,
            color = if (isActive) activeColor else Color.Gray,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── MICROPHONE PERMISSION BANNER ─────────────────────────────────────────────
@Composable
private fun MicrophonePermissionBanner(
    onGrantClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NexBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NexOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MicOff,
                    contentDescription = null,
                    tint = NexOrange,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Microphone Disabled",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexTextPrimary
                )
                Text(
                    "Permission needed to talk in room.",
                    fontSize = 11.sp,
                    color = NexTextSecondary
                )
            }

            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexGold,
                    contentColor = NexOnAccent
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Allow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = NexTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── ROOM OPTIONS BOTTOM SHEET ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomOptionsSheet(
    roomId: String,
    roomName: String,
    onDismiss: () -> Unit,
    onInvite: () -> Unit,
    onLeave: () -> Unit
) {
    val ctx = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                roomName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SheetOption(
                icon = Icons.Default.ContentCopy,
                label = "Copy Room Code",
                tint = Color(0xFF4CAF50)
            ) {
                val clip = ctx.getSystemService(android.content.ClipboardManager::class.java)
                clip?.setPrimaryClip(android.content.ClipData.newPlainText("Room Code", roomId))
                Toast.makeText(ctx, "Room Code copied!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }

            SheetOption(
                icon = Icons.Default.Share,
                label = "Share Invite Link",
                tint = Color(0xFF2196F3)
            ) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, InviteLink.shareMessage(roomName, roomId))
                }
                ctx.startActivity(Intent.createChooser(send, "Share invite via…"))
                onDismiss()
            }

            SheetOption(
                icon = Icons.Default.PersonAdd,
                label = "Add Member",
                tint = Color(0xFF4CAF50)
            ) { onInvite() }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(4.dp))

            SheetOption(
                icon = Icons.Default.CallEnd,
                label = "Leave Room",
                tint = Color(0xFFE57373),
                labelColor = Color(0xFFE57373)
            ) { onLeave() }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    labelColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(tint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 15.sp, color = labelColor, fontWeight = FontWeight.Medium)
    }
}

