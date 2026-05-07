package com.example.rgbbluetoothcontrol.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbbluetoothcontrol.bt.BtViewModel
import com.example.rgbbluetoothcontrol.bt.ConnectionState
import com.example.rgbbluetoothcontrol.bt.isBusy
import com.example.rgbbluetoothcontrol.bt.isConnected

@Composable
fun MainScreen(vm: BtViewModel = viewModel()) {
    val state by vm.connectionState.collectAsStateWithLifecycle()
    val color by vm.color.collectAsStateWithLifecycle()
    val ledOn by vm.ledOn.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val lastTx by vm.lastTx.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showDevicePicker by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<((BluetoothDevice) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showDevicePicker = true
    }

    LaunchedEffect(Unit) {
        vm.snackbar.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(Unit) {
        if (hasBluetoothPermission(context)) vm.tryAutoReconnect()
    }

    if (showDevicePicker) {
        DevicePickerDialog(
            devices = vm.getBondedDevices(),
            onDismiss = { showDevicePicker = false },
            onPick = { device ->
                showDevicePicker = false
                pendingAction?.invoke(device)
                pendingAction = null
            },
        )
    }

    fun requestPick(onPicked: (BluetoothDevice) -> Unit) {
        pendingAction = onPicked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            showDevicePicker = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppBar(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

            StatusCard(
                state = state,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            ActionRow(
                state = state,
                onConnect = { requestPick { vm.connectTo(it) } },
                onReconnect = { vm.reconnect() },
                onChangeDevice = { requestPick { vm.connectTo(it) } },
                modifier = Modifier.padding(bottom = 16.dp),
            )

            val live = state.isConnected && ledOn
            ColorWheelCanvas(
                color = color,
                enabled = live,
                onChange = { vm.onColorChange(it) },
                onSettle = { vm.onColorSettle(it) },
                overlayText = when (state) {
                    is ConnectionState.Reconnecting -> "Reconnecting…"
                    is ConnectionState.Connecting -> "Connecting…"
                    else -> null
                },
                modifier = Modifier.padding(bottom = 18.dp),
            )

            RecentColorsRow(
                recents = recents,
                live = live,
                onPickRecent = { vm.pickRecent(it) },
                modifier = Modifier.padding(bottom = 12.dp),
            )

            ColorReadoutCard(
                color = color,
                connected = state.isConnected,
                ledOn = ledOn,
                onToggleLed = { vm.setLedOn(!ledOn) },
                modifier = Modifier.padding(bottom = 4.dp),
            )

            TxIndicator(
                state = state,
                lastTx = lastTx,
                ledOn = ledOn,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun AppBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "◉",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RGB Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "HC-06 · Croduino",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DevicePickerDialog(
    devices: List<BluetoothDevice>,
    onDismiss: () -> Unit,
    onPick: (BluetoothDevice) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bluetooth Device") },
        text = {
            if (devices.isEmpty()) {
                Text(
                    text = "No paired devices found.\n\nPair your HC-06 in system Bluetooth settings first.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn {
                    items(devices) { device ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(device) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name ?: "Unknown device",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(20.dp),
    )
}

private fun hasBluetoothPermission(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else true
