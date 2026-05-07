package com.example.rgbbluetoothcontrol.bt

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rgbbluetoothcontrol.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlin.math.roundToInt

class BtViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val LAST_DEVICE_KEY = stringPreferencesKey("last_device")
        private val RECENT_COLORS_KEY = stringPreferencesKey("recent_colors")
    }

    private val bluetoothAdapter: BluetoothAdapter? =
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _color = MutableStateFlow(HsvColor(180f, 0.8f, 1f))
    val color: StateFlow<HsvColor> = _color.asStateFlow()

    private val _ledOn = MutableStateFlow(false)
    val ledOn: StateFlow<Boolean> = _ledOn.asStateFlow()

    private val _recents = MutableStateFlow<List<HsvColor>>(emptyList())
    val recents: StateFlow<List<HsvColor>> = _recents.asStateFlow()

    private val _lastTx = MutableStateFlow("")
    val lastTx: StateFlow<String> = _lastTx.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var lastSentRgb: Triple<Int, Int, Int>? = null

    private var sendJob: Job? = null
    private var recentsDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            getApplication<Application>().dataStore.data
                .catch { /* ignore corrupted prefs */ }
                .map { it[RECENT_COLORS_KEY] }
                .firstOrNull()
                ?.let { encoded -> _recents.value = decodeColors(encoded) }
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<BluetoothDevice> =
        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

    fun connectTo(device: BluetoothDevice) {
        if (_connectionState.value.isBusy) return
        viewModelScope.launch(Dispatchers.IO) {
            closeSocket()
            openSocket(device)
        }
    }

    fun reconnect() {
        val current = _connectionState.value as? ConnectionState.Connected ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _connectionState.value = ConnectionState.Reconnecting(current.device)
            closeSocket()
            openSocket(current.device)
        }
    }

    fun onColorChange(color: HsvColor) {
        sendJob?.cancel()
        _color.value = color
        if (!isLive()) return
        sendJob = viewModelScope.launch(Dispatchers.IO) {
            delay(50)
            currentCoroutineContext().ensureActive()
            sendColorNow(color)
        }
    }

    fun onColorSettle(color: HsvColor) {
        sendJob?.cancel()
        _color.value = color
        scheduleAddToRecents(color)
        if (!isLive()) return
        sendJob = viewModelScope.launch(Dispatchers.IO) {
            smoothFadeTo(hsvToRgb(color))
        }
    }

    fun pickRecent(color: HsvColor) {
        if (!isLive()) return
        sendJob?.cancel()
        _color.value = color
        sendJob = viewModelScope.launch(Dispatchers.IO) {
            smoothFadeTo(hsvToRgb(color))
        }
    }

    fun setLedOn(on: Boolean) {
        sendJob?.cancel()
        _ledOn.value = on
        if (_connectionState.value !is ConnectionState.Connected) return
        sendJob = viewModelScope.launch(Dispatchers.IO) {
            if (on) sendColorNow(_color.value) else sendRgb(0, 0, 0)
        }
    }

    fun tryAutoReconnect() {
        viewModelScope.launch {
            val addr = getApplication<Application>().dataStore.data
                .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
                .map { it[LAST_DEVICE_KEY] }
                .firstOrNull() ?: return@launch
            val device = getBondedDevices().find { it.address == addr } ?: return@launch
            connectTo(device)
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeSocket()
    }

    // ── Private helpers ─────────────────────────────────────────

    private fun isLive() =
        _connectionState.value is ConnectionState.Connected && _ledOn.value

    @SuppressLint("MissingPermission")
    private suspend fun openSocket(device: BluetoothDevice) {
        _connectionState.value = when (_connectionState.value) {
            is ConnectionState.Reconnecting -> ConnectionState.Reconnecting(device)
            else -> ConnectionState.Connecting(device)
        }
        try {
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothAdapter?.cancelDiscovery()
            s.connect()
            socket = s
            outputStream = s.outputStream
            _connectionState.value = ConnectionState.Connected(device)
            _ledOn.value = true
            saveLastDevice(device.address)
        } catch (e: Exception) {
            closeSocket()
            _connectionState.value = ConnectionState.Disconnected
            withContext(Dispatchers.Main) {
                _snackbar.emit("Connection failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun closeSocket() {
        runCatching { outputStream?.close() }
        runCatching { socket?.close() }
        outputStream = null
        socket = null
    }

    private suspend fun sendColorNow(color: HsvColor) {
        val (r, g, b) = hsvToRgb(color)
        sendRgb(r, g, b)
    }

    private suspend fun sendRgb(r: Int, g: Int, b: Int) {
        val msg = "$r,$g,$b#"
        try {
            outputStream?.write(msg.toByteArray())
            lastSentRgb = Triple(r, g, b)
            _lastTx.value = msg
        } catch (e: IOException) {
            handleUnexpectedDisconnect()
        }
    }

    private suspend fun smoothFadeTo(target: Triple<Int, Int, Int>) {
        val from = lastSentRgb ?: target
        for (step in 1..10) {
            currentCoroutineContext().ensureActive()
            val t = step.toFloat() / 10f
            val r = lerp(from.first, target.first, t)
            val g = lerp(from.second, target.second, t)
            val b = lerp(from.third, target.third, t)
            sendRgb(r, g, b)
            delay(20)
        }
    }

    private fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t).roundToInt()

    private suspend fun handleUnexpectedDisconnect() {
        closeSocket()
        val wasConnected = _connectionState.value is ConnectionState.Connected
        _connectionState.value = ConnectionState.Disconnected
        _ledOn.value = false
        _lastTx.value = ""
        lastSentRgb = null
        if (!wasConnected) return

        withContext(Dispatchers.Main) {
            _snackbar.emit("Disconnected — attempting to reconnect…")
        }
        delay(1000)
        // Auto-reconnect once
        val addr = getApplication<Application>().dataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { it[LAST_DEVICE_KEY] }
            .firstOrNull() ?: return
        val device = getBondedDevices().find { it.address == addr } ?: return
        openSocket(device)
    }

    private fun scheduleAddToRecents(color: HsvColor) {
        recentsDebounceJob?.cancel()
        recentsDebounceJob = viewModelScope.launch {
            delay(250)
            addToRecents(color)
        }
    }

    private fun addToRecents(color: HsvColor) {
        val key = { c: HsvColor -> "${c.h.roundToInt()}-${(c.s * 10).roundToInt()}" }
        val deduped = _recents.value.filter { key(it) != key(color) }
        _recents.value = (listOf(color) + deduped).take(5)
        viewModelScope.launch {
            saveRecents(_recents.value)
        }
    }

    private suspend fun saveLastDevice(address: String) {
        getApplication<Application>().dataStore.edit { it[LAST_DEVICE_KEY] = address }
    }

    private suspend fun saveRecents(colors: List<HsvColor>) {
        getApplication<Application>().dataStore.edit { prefs ->
            prefs[RECENT_COLORS_KEY] = colors.joinToString(",") { "${it.h}:${it.s}:${it.v}" }
        }
    }

    private fun decodeColors(encoded: String): List<HsvColor> =
        encoded.split(",").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size == 3) runCatching {
                HsvColor(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat())
            }.getOrNull() else null
        }
}
