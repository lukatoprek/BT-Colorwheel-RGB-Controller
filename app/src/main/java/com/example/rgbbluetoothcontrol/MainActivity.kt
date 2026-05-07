package com.example.rgbbluetoothcontrol

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.rgbbluetoothcontrol.ui.MainScreen
import com.example.rgbbluetoothcontrol.ui.theme.RGBBluetoothControlTheme

class MainActivity : ComponentActivity() {

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions.launch(bluetoothPermissions)
        enableEdgeToEdge()
        setContent {
            RGBBluetoothControlTheme {
                MainScreen()
            }
        }
    }
}
