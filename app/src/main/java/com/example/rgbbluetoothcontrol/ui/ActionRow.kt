package com.example.rgbbluetoothcontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rgbbluetoothcontrol.bt.ConnectionState
import com.example.rgbbluetoothcontrol.bt.isBusy
import com.example.rgbbluetoothcontrol.bt.isConnected

@Composable
fun ActionRow(
    state: ConnectionState,
    onConnect: () -> Unit,
    onReconnect: () -> Unit,
    onChangeDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val busy = state.isBusy
    val connected = state.isConnected
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = if (connected) onReconnect else onConnect,
            enabled = !busy,
            shape = shape,
            modifier = Modifier.weight(1f).height(44.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                text = when {
                    busy -> "…"
                    connected -> "Reconnect"
                    else -> "Connect"
                },
            )
        }

        OutlinedButton(
            onClick = onChangeDevice,
            enabled = !busy,
            shape = shape,
            modifier = Modifier.weight(1f).height(44.dp),
        ) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Change device")
        }
    }
}
