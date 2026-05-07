package com.example.rgbbluetoothcontrol.bt

import android.bluetooth.BluetoothDevice

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val device: BluetoothDevice) : ConnectionState()
    data class Connected(val device: BluetoothDevice) : ConnectionState()
    data class Reconnecting(val device: BluetoothDevice) : ConnectionState()
}

val ConnectionState.isBusy
    get() = this is ConnectionState.Connecting || this is ConnectionState.Reconnecting

val ConnectionState.isConnected
    get() = this is ConnectionState.Connected

val ConnectionState.btDevice: BluetoothDevice?
    get() = when (this) {
        is ConnectionState.Connecting -> device
        is ConnectionState.Connected -> device
        is ConnectionState.Reconnecting -> device
        else -> null
    }
