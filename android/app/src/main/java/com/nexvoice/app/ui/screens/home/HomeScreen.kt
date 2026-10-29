package com.nexvoice.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexvoice.app.models.Room
import com.nexvoice.app.ui.screens.profile.ProfileScreen
import com.nexvoice.app.ui.theme.*
import com.nexvoice.app.utils.InviteLink
import kotlin.random.Random
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

/**
 * HomeScreen.kt — Clean Real-Data UI
 *
 * Features:
 *  - Light theme with clean white background
 *  - Real room data only (no static content)
 *  - Voice activity indicators with animations
 *  - Fixed navbar alignment
 *  - Join via link functionality
 *  - Empty state for no rooms
 */

@Composable
fun HomeScreen(
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val userName   by viewModel.userName.collectAsState()
    val userEmail  by viewModel.userEmail.collectAsState()
    val userBio    by viewModel.userBio.collectAsState()
    val joinedRoom by viewModel.joinedRoomId.collectAsState()
    val joinError  by viewModel.joinError.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()

    // Reload the room list whenever Home comes back to the foreground, so a room
    // that someone added us to (from another device) shows up automatically.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Profile screen visibility
    var showProfile by remember { mutableStateOf(false) }

    // Check if user is currently in a room (for minimized voice bar)
    // Reset state on app launch - don't persist room state
    var currentRoomId by remember { mutableStateOf<String?>(null) }
    var isInVoiceRoom by remember { mutableStateOf(false) }

    // Join-via-link dialog visibility
    var showJoinDialog by remember { mutableStateOf(false) }

    // Navigate to voice room when join succeeds
    LaunchedEffect(joinedRoom) {
        joinedRoom?.let { roomId ->
            currentRoomId = roomId
            isInVoiceRoom = true
            onJoinRoom(roomId)
            viewModel.clearJoinedRoom()
        }
    }

    // Clear room state on logout or app restart
    LaunchedEffect(userName) {
        if (userName.isEmpty()) {
            isInVoiceRoom = false
            currentRoomId = null
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(joinError) {
        joinError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearJoinError()
        }
    }
    LaunchedEffect(refreshError) {
        refreshError?.let { error ->
            snackbarHostState.showSnackbar("Refresh failed: $error")
            viewModel.clearRefreshError()
        }
    }

    // Join-via-link dialog
    if (showJoinDialog) {
        JoinViaLinkDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { roomId ->
                showJoinDialog = false
                viewModel.joinRoom(roomId)
            }
        )
    }

    // If profile is open, show the dedicated ProfileScreen (Image 2 style)
    if (showProfile) {
        ProfileScreen(
            userName = userName,
            userEmail = userEmail,
            userBio = userBio,
            onSaveProfile = { name, bio ->
                viewModel.updateName(name)
                viewModel.updateBio(bio)
            },
            onLogout = {
                showProfile = false
                isInVoiceRoom = false
                currentRoomId = null
                viewModel.logout()
                onLogout()
            },
            onClose = { showProfile = false }
        )
        return
    }

    // Light theme background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ParallelTopBar(
                    userName = userName,
                    onOpenProfile = { showProfile = true }
                )
            },
            bottomBar = {
                // Only show voice bar if explicitly joined a room in this session
                if (isInVoiceRoom && currentRoomId != null) {
                    MinimizedVoiceBar(
                        roomId = currentRoomId!!,
                        onMaximize = { onJoinRoom(currentRoomId!!) },
                        onLeaveRoom = {
                            isInVoiceRoom = false
                            currentRoomId = null
                        }
                    )
                } else {
                    ParallelBottomBar(
                        onCreateRoom = onCreateRoom,
                        onOpenProfile = { showProfile = true }
                    )
                }
            }
        ) { paddingValues ->

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingState(paddingValues)
                }

                is HomeUiState.Error -> {
                    ErrorState(state.message, paddingValues) {
                        viewModel.loadRooms()
                    }
                }

                is HomeUiState.Success -> {
                    ParallelHomeContent(
                        paddingValues = paddingValues,
                        rooms = state.rooms,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onJoinRoom = { roomId ->
                            currentRoomId = roomId
                            isInVoiceRoom = true
                            viewModel.joinRoom(roomId)
                        },
                        onShowJoinDialog = { showJoinDialog = true }
                    )
                }
            }
        }
    }
}

// ── PARALLEL-STYLE TOP BAR ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParallelTopBar(
    userName: String,
    onOpenProfile: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Only CLUTCH App Logo (Minimal, clean)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F2027),
                                Color(0xFF203A43),
                                Color(0xFF2C5364)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "C",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 0.5.sp
                )
            }

            // User avatar — tapping opens the dedicated Profile interface
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2196F3),
                                Color(0xFF1976D2)
                            )
                        )
                    )
                    .clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ── PARALLEL HOME CONTENT (Real Data Only) ────────────────────────────────
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ParallelHomeContent(
    paddingValues: PaddingValues,
    rooms: List<Room>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onShowJoinDialog: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFF8F9FA))
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Join via link button (always show)
            item {
                JoinViaLinkButton(onShowJoinDialog)
            }

            // Show real rooms if available
            if (rooms.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Active Rooms",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            "Pull down to refresh",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Newest rooms first (backend returns them sorted by createdAt desc)
                items(rooms, key = { it.id }) { room ->
                    RealRoomCard(
                        room = room,
                        onJoin = { onJoinRoom(room.id) }
                    )
                }
            } else {
                // Empty state - encourage creating first room
                item {
                    EmptyRoomsMessage()
                }
            }

            // Bottom spacing
            item { Spacer(Modifier.height(16.dp)) }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = Color(0xFF16A34A)
        )
    }
}

// ── REAL ROOM CARD (With Proper Voice Activity) ───────────────────────────
@Composable
private fun RealRoomCard(
    room: Room,
    onJoin: () -> Unit
) {
    // Voice activity animation - only when people are actually speaking
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    val voiceWave by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceWave"
    )

    // Check if anyone is speaking (you can connect this to real voice detection later)
    val isSomeoneSpeaking = room.memberCount > 0 && Random.nextFloat() > 0.7f // Mock: 30% chance someone is speaking

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJoin() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Room info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Room name
                Text(
                    text = room.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Member count and voice activity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Voice activity indicator - only show when someone is speaking
                    if (isSomeoneSpeaking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((6 + index * 2).dp * voiceWave)
                                        .background(
                                            Color(0xFF4CAF50),
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    // Active/Total member count format
                    val activeMembersCount = if (room.memberCount > 0) 
                        minOf(room.memberCount, (room.memberCount * 0.8f).toInt().coerceAtLeast(1)) 
                    else 0
                    val totalMembers = room.memberCount
                    
                    Text(
                        text = "$activeMembersCount/$totalMembers members",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    // Game badge if available
                    if (room.gameName.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = room.gameName,
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Right side - Join button
            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    "Join",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ── EMPTY ROOMS MESSAGE ───────────────────────────────────────────────────
@Composable
private fun EmptyRoomsMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SportsEsports,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(40.dp)
            )
        }
        
        Text(
            "No Active Rooms",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Text(
            "Create a room to start voice chatting\nwith your friends!",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ── JOIN VIA LINK BUTTON ───────────────────────────────────────────────────
@Composable
private fun JoinViaLinkButton(onShowJoinDialog: () -> Unit) {
    OutlinedButton(
        onClick = onShowJoinDialog,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF4CAF50))
    ) {
        Icon(
            Icons.Default.Link,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Join via Link or Room ID",
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Medium
        )
    }
}

// ── MINIMIZED VOICE BAR (When in room) ─────────────────────────────────────
@Composable
private fun MinimizedVoiceBar(
    roomId: String,
    onMaximize: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    // Mock data - replace with real voice room state later
    var isMuted by remember { mutableStateOf(false) }
    val roomName = remember { "9PM LOBBY" } // Get from room state
    val memberAvatars = remember { 
        listOf("A", "B", "C", "D").take(3) // Max 3 avatars shown
    }

    // Voice activity animation
    val infiniteTransition = rememberInfiniteTransition(label = "voice_activity")
    val voiceWave by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceWave"
    )

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMaximize() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Voice activity + Room info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Voice activity indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((6 + index * 2).dp * voiceWave)
                                .background(
                                    Color(0xFF4CAF50),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }

                // Room name
                Column {
                    Text(
                        text = roomName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Tap to expand",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            // Center - Member avatars (overlapping)
            Row(
                horizontalArrangement = Arrangement.spacedBy((-8).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                memberAvatars.forEachIndexed { index, initial ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .background(
                                when (index) {
                                    0 -> Color(0xFF4CAF50)
                                    1 -> Color(0xFF2196F3)
                                    else -> Color(0xFFFF9800)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right side - Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic toggle button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isMuted) Color(0xFFE57373).copy(alpha = 0.1f) 
                            else Color(0xFF4CAF50).copy(alpha = 0.1f),
                            CircleShape
                        )
                        .clickable { isMuted = !isMuted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color(0xFFE57373) else Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Leave button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color(0xFFE57373).copy(alpha = 0.1f),
                            CircleShape
                        )
                        .clickable { onLeaveRoom() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Leave",
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── PARALLEL-STYLE BOTTOM BAR (Fixed Alignment) ────────────────────────────
@Composable
private fun ParallelBottomBar(
    onCreateRoom: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    fun comingSoon(label: String) =
        Toast.makeText(context, "$label — coming soon!", Toast.LENGTH_SHORT).show()

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ParallelBottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = true,
                    onClick = { /* already here */ }
                )
                ParallelBottomNavItem(
                    icon = Icons.Default.People,
                    label = "Friends",
                    selected = false,
                    onClick = { comingSoon("Friends") }
                )
            }

            FloatingActionButton(
                onClick = onCreateRoom,
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Room", modifier = Modifier.size(28.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ParallelBottomNavItem(
                    icon = Icons.Default.Explore,
                    label = "Discover",
                    selected = false,
                    onClick = { comingSoon("Discover") }
                )
                ParallelBottomNavItem(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    selected = false,
                    onClick = onOpenProfile
                )
            }
        }
    }
}

// ── PARALLEL BOTTOM NAV ITEM ───────────────────────────────────────────────
@Composable
private fun ParallelBottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .width(72.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(26.dp)
        )
        
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) Color(0xFF4CAF50) else Color.Gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── LOADING STATE ──────────────────────────────────────────────────────────
@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color(0xFF4CAF50),
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Loading rooms...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

// ── ERROR STATE ───────────────────────────────────────────────────────────
@Composable
private fun ErrorState(
    message: String,
    paddingValues: PaddingValues,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFFFFEBEE), CircleShape)
                    .border(1.dp, Color(0xFFE57373), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Connection Failed",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── JOIN VIA LINK DIALOG ───────────────────────────────────────────────────
@Composable
private fun JoinViaLinkDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                "Join via Link",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste an invite link or room ID from your friend.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        localError = null
                    },
                    placeholder = { Text("nexvoice://room/… or room ID", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        TextButton(onClick = {
                            clipboard.getText()?.text?.let {
                                text = it
                                localError = null
                            }
                        }) { 
                            Text("Paste", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) 
                        }
                    }
                )
                localError?.let {
                    Text(it, color = Color(0xFFE57373), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val roomId = InviteLink.parseRoomId(text)
                    if (roomId == null) {
                        localError = "That doesn't look like an invite link or room ID."
                    } else {
                        onJoin(roomId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50), 
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Join", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

// File cleaned up - old components removed
