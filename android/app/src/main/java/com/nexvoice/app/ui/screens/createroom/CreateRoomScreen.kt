package com.nexvoice.app.ui.screens.createroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexvoice.app.ui.theme.*

/**
 * CreateRoomScreen.kt (Phase 3)
 *
 * Now calls the real backend API to create rooms.
 * Shows a loading spinner on the button while waiting.
 * Navigates to VoiceRoomScreen with the real room ID on success.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    onRoomCreated: (roomId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateRoomViewModel = viewModel()
) {
    var roomName  by remember { mutableStateOf("") }
    var gameName  by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    // Navigate when room is created successfully
    LaunchedEffect(uiState) {
        if (uiState is CreateRoomUiState.Success) {
            val roomId = (uiState as CreateRoomUiState.Success).roomId
            viewModel.resetState()
            onRoomCreated(roomId)
        }
    }

    val isLoading    = uiState is CreateRoomUiState.Loading
    val errorMessage = (uiState as? CreateRoomUiState.Error)?.message ?: ""

    Scaffold(
        containerColor = NexBg,
        topBar = {
            TopAppBar(
                title = { Text("Create Room", color = NexTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = NexGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexBg)
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexSurface)
                    .border(1.dp, NexBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                @Composable
                fun SectionLabel(text: String) {
                    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NexGold, letterSpacing = 1.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("ROOM NAME")
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        placeholder = { Text("e.g. Ranked Grind Squad", color = NexTextSecondary) },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = nexTextFieldColors(),
                        leadingIcon = { Icon(Icons.Default.MicNone, null, tint = NexGold) }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("GAME")
                    OutlinedTextField(
                        value = gameName,
                        onValueChange = { gameName = it },
                        placeholder = { Text("e.g. Valorant", color = NexTextSecondary) },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = nexTextFieldColors(),
                        leadingIcon = { Icon(Icons.Default.SportsEsports, null, tint = NexGold) }
                    )
                }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = NexMuted, fontSize = 14.sp)
            }

            Button(
                onClick = {
                    if (roomName.isBlank() || gameName.isBlank()) return@Button
                    viewModel.createRoom(
                        name       = roomName.trim(),
                        gameName   = gameName.trim()
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexGold, contentColor = NexOnAccent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NexOnAccent, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Launch Room", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
    }
}

@Composable
private fun nexTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = NexGold,
    unfocusedBorderColor    = NexBorder,
    focusedTextColor        = NexTextPrimary,
    unfocusedTextColor      = NexTextPrimary,
    cursorColor             = NexGold,
    focusedContainerColor   = NexSurfaceAlt,
    unfocusedContainerColor = NexSurfaceAlt
)

