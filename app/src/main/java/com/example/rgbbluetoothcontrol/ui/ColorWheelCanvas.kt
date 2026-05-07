package com.example.rgbbluetoothcontrol.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rgbbluetoothcontrol.bt.HsvColor
import com.example.rgbbluetoothcontrol.bt.hsvToRgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.foundation.Canvas as ComposeCanvas

private const val MARKER_RADIUS_DP = 14f

private val hueColors = listOf(
    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
)

@Composable
fun ColorWheelCanvas(
    color: HsvColor,
    enabled: Boolean,
    onChange: (HsvColor) -> Unit,
    onSettle: (HsvColor) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    overlayText: String? = null,
) {
    val onChangeRef by rememberUpdatedState(onChange)
    val onSettleRef by rememberUpdatedState(onSettle)

    var localColor by remember { mutableStateOf(color) }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        ComposeCanvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                // Washed-out overlay when disabled
                .drawWithContent {
                    drawContent()
                    if (!enabled) drawRect(Color.White.copy(alpha = 0.7f))
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        localColor = pickColor(down.position.x, down.position.y, size.toPx())
                        onChangeRef(localColor)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                onSettleRef(localColor)
                                break
                            }
                            if (change.position != change.previousPosition) {
                                change.consume()
                                localColor = pickColor(change.position.x, change.position.y, size.toPx())
                                onChangeRef(localColor)
                            }
                        }
                    }
                },
        ) {
            val radiusPx = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            drawWheel(radiusPx, center)

            if (enabled) {
                drawMarker(color, radiusPx, center)
            }
        }

        if (overlayText != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(overlayText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (!enabled && overlayText == null) {
            LockOverlay()
        }
    }
}

private fun DrawScope.drawWheel(radiusPx: Float, center: Offset) {
    // Hue ring: rotate -90° so hue=0 (red) sits at the top
    rotate(-90f, pivot = center) {
        drawCircle(
            brush = Brush.sweepGradient(colors = hueColors, center = center),
            radius = radiusPx,
            center = center,
        )
    }
    // Saturation overlay: white at center fading to transparent
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color.Transparent),
            center = center,
            radius = radiusPx,
        ),
        radius = radiusPx,
        center = center,
    )
}

private fun DrawScope.drawMarker(color: HsvColor, radiusPx: Float, center: Offset) {
    val markerPx = MARKER_RADIUS_DP.dp.toPx()
    val angle = (color.h - 90f) * (PI / 180.0)
    val dist = color.s * (radiusPx - markerPx)
    val markerPos = Offset(
        x = center.x + dist * cos(angle).toFloat(),
        y = center.y + dist * sin(angle).toFloat(),
    )
    val (r, g, b) = hsvToRgb(color)

    drawCircle(
        color = Color(r / 255f, g / 255f, b / 255f),
        radius = markerPx,
        center = markerPos,
    )
    drawCircle(
        color = Color.White,
        radius = markerPx,
        center = markerPos,
        style = Stroke(width = 3.dp.toPx()),
    )
}

private fun pickColor(touchX: Float, touchY: Float, sizePx: Float): HsvColor {
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val dx = touchX - cx
    val dy = touchY - cy
    val maxR = sizePx / 2f
    val dist = sqrt(dx * dx + dy * dy).coerceAtMost(maxR)
    var h = (atan2(dy, dx) * 180.0 / PI + 90.0).toFloat() % 360f
    if (h < 0f) h += 360f
    return HsvColor(h, dist / maxR, 1f)
}

@Composable
private fun LockOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val lockColor = Color(0f, 0f, 0f, 0.55f)
            ComposeCanvas(modifier = Modifier.size(36.dp)) {
                val w = size.width
                val h = size.height
                val strokeW = 3.dp.toPx()

                // Lock body
                drawRoundRect(
                    color = lockColor,
                    topLeft = Offset(w * 0.18f, h * 0.46f),
                    size = Size(w * 0.64f, h * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
                // Lock shackle (arc)
                drawArc(
                    color = lockColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.27f, h * 0.08f),
                    size = Size(w * 0.46f, h * 0.46f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                )
            }
            Text(
                text = "Connect a device",
                style = MaterialTheme.typography.bodySmall,
                color = lockColor,
            )
        }
    }
}

