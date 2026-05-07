package com.example.rgbbluetoothcontrol.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rgbbluetoothcontrol.bt.HsvColor
import com.example.rgbbluetoothcontrol.bt.hsvToRgb
import com.example.rgbbluetoothcontrol.ui.theme.MonoStyle

@Composable
fun RecentColorsRow(
    recents: List<HsvColor>,
    live: Boolean,
    onPickRecent: (HsvColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowAlpha by animateFloatAsState(
        targetValue = if (live) 1f else 0.4f,
        label = "recentAlpha",
    )
    val slotShape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "RECENT",
            style = MonoStyle.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val displayCount = 5

            recents.take(displayCount).forEach { c ->
                val (r, g, b) = hsvToRgb(c)
                val swatchColor = Color(r / 255f, g / 255f, b / 255f)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(slotShape)
                        .drawBehind { drawRect(color = swatchColor) }
                        .then(
                            if (live) Modifier.clickable { onPickRecent(c) } else Modifier
                        ),
                )
            }

            // Empty placeholder slots
            repeat((displayCount - recents.size).coerceAtLeast(0)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(slotShape)
                        .drawBehind {
                            drawRoundRect(
                                color = Color.Gray.copy(alpha = 0.12f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                            )
                        }
                        .border(1.dp, MaterialTheme.colorScheme.outline, slotShape),
                )
            }
        }
    }
}
