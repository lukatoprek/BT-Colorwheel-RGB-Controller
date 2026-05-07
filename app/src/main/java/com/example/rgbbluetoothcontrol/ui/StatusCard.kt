package com.example.rgbbluetoothcontrol.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rgbbluetoothcontrol.bt.ConnectionState
import com.example.rgbbluetoothcontrol.bt.isBusy
import com.example.rgbbluetoothcontrol.bt.isConnected
import com.example.rgbbluetoothcontrol.ui.theme.MonoStyle

private val SuccessGreen = Color(0xFF4F7D4A)
private val WarningAmber = Color(0xFFB26A00)
private val ErrorRed = Color(0xFFBA1A1A)

@Composable
fun StatusCard(state: ConnectionState, modifier: Modifier = Modifier) {
    val (label, dotColor, subtitle) = when (state) {
        is ConnectionState.Connected ->
            Triple("Connected", SuccessGreen, state.device.name ?: state.device.address)
        is ConnectionState.Connecting ->
            Triple("Connecting…", WarningAmber, state.device.name ?: state.device.address)
        is ConnectionState.Reconnecting ->
            Triple("Reconnecting…", WarningAmber, state.device.name ?: state.device.address)
        ConnectionState.Disconnected ->
            Triple("Not connected", ErrorRed, "Select a device to begin")
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusDot(color = dotColor, connected = state.isConnected, busy = state.isBusy)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = if (state.isConnected) "9600 bps" else "—",
                style = MonoStyle.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color, connected: Boolean, busy: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")

    // Pulse halo for connected, blink alpha for busy, static for disconnected
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "halo",
    )
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "blink",
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .drawBehind {
                if (connected) {
                    drawCircle(color = color.copy(alpha = haloAlpha), radius = size.minDimension / 2f + 6.dp.toPx())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(color = color.copy(alpha = if (busy) blinkAlpha else 1f))
                },
        )
    }
}
