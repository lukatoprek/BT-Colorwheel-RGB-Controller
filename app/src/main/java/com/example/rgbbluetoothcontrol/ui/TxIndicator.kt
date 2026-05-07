package com.example.rgbbluetoothcontrol.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rgbbluetoothcontrol.bt.ConnectionState
import com.example.rgbbluetoothcontrol.bt.isConnected
import com.example.rgbbluetoothcontrol.ui.theme.MonoStyle

@Composable
fun TxIndicator(
    state: ConnectionState,
    lastTx: String,
    ledOn: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = when {
        !state.isConnected -> "—"
        lastTx.isNotEmpty() && ledOn -> "→ sent  \"$lastTx\""
        else -> "→ idle"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MonoStyle.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
