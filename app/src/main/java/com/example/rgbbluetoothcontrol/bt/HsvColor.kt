package com.example.rgbbluetoothcontrol.bt

import kotlin.math.abs
import kotlin.math.roundToInt

data class HsvColor(val h: Float, val s: Float, val v: Float = 1f)

fun hsvToRgb(color: HsvColor): Triple<Int, Int, Int> {
    val (h, s, v) = color
    val c = v * s
    val hh = (h / 60f) % 6f
    val x = c * (1f - abs(hh % 2f - 1f))
    var r = 0f; var g = 0f; var b = 0f
    when {
        hh < 1f -> { r = c; g = x }
        hh < 2f -> { r = x; g = c }
        hh < 3f -> { g = c; b = x }
        hh < 4f -> { g = x; b = c }
        hh < 5f -> { r = x; b = c }
        else    -> { r = c; b = x }
    }
    val m = v - c
    return Triple(
        ((r + m) * 255f).roundToInt().coerceIn(0, 255),
        ((g + m) * 255f).roundToInt().coerceIn(0, 255),
        ((b + m) * 255f).roundToInt().coerceIn(0, 255),
    )
}

fun rgbToHex(r: Int, g: Int, b: Int) = "#%02X%02X%02X".format(r, g, b)
