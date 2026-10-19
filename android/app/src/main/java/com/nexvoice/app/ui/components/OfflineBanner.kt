package com.nexvoice.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexvoice.app.ui.theme.*

/**
 * OfflineBanner.kt
 *
 * Reusable banner that shows when backend is unreachable.
 * Can be placed at top of any screen.
 *
 * Usage:
 *   val isOnline by remember { mutableStateOf(true) }
 *   OfflineBanner(
 *       isVisible = !isOnline,
 *       onRetry = { checkConnection() }
 *   )
 */
@Composable
fun OfflineBanner(
    isVisible: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "You're offline. Connecting…",
    buttonText: String = "Retry Now"
) {
    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                NexMuted.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = NexMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                color = NexTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexGold,
                    contentColor = NexOnAccent
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}