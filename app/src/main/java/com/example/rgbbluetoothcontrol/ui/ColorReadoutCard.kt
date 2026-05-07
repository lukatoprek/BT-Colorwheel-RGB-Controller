package com.example.rgbbluetoothcontrol.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rgbbluetoothcontrol.bt.HsvColor
import com.example.rgbbluetoothcontrol.bt.hsvToRgb
import com.example.rgbbluetoothcontrol.bt.rgbToHex
import com.example.rgbbluetoothcontrol.ui.theme.MonoStyle

@Composable
fun ColorReadoutCard(
    color: HsvColor,
    connected: Boolean,
    ledOn: Boolean,
    onToggleLed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val live = connected && ledOn
    val (r, g, b) = hsvToRgb(color)
    val displayColor = if (live) Color(r / 255f, g / 255f, b / 255f) else Color(0xFFD6D3CC)

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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .drawBehind { drawRect(displayColor) },
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (live) rgbToHex(r, g, b) else "#——————",
                    style = MonoStyle.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (live) "$r, $g, $b#" else "R, G, B#",
                    style = MonoStyle.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onToggleLed,
                enabled = connected,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = if (ledOn && connected) Color(0xFF475D50) else Color(0xFFF1EEE8)
                        )
                    },
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = if (ledOn) "Turn LED off" else "Turn LED on",
                    tint = if (ledOn && connected) Color.White else Color(0xFF5C5B56),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
