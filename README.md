# RGB Bluetooth Control

An Android app for controlling an RGB LED connected to a Croduino Basic3 microcontroller via Bluetooth Classic (HC-06).

![App Screenshot](https://i.imgur.com/PdntNmu.jpeg)

## What it does

Connect to an HC-06 Bluetooth module over SPP, pick a color using an interactive color wheel, and the app sends RGB values to the microcontroller in real time. The LED changes color accordingly.

## Hardware

| Component | Details |
|-----------|---------|
| Microcontroller | Croduino Basic3 (Arduino Uno-compatible) |
| Bluetooth module | HC-06 (SPP, 9600 baud) |
| RGB LED | Common cathode — R: pin 9, G: pin 6, B: pin 5 (PWM) |

**Wire protocol:** ASCII string `R,G,B#` — e.g. `255,128,0#`

## Technologies

- **Kotlin** — primary language
- **Jetpack Compose** — UI (single-screen)
- **Material 3** — design system
- **Bluetooth Classic (SPP)** — `android.bluetooth` API
- **AndroidX DataStore** — persisting last device + recent colors
- **ViewModel + StateFlow** — UI state management

## UI Components

| Component | Description |
|-----------|-------------|
| Status card | Connection state with animated indicator dot |
| Action row | Connect / Reconnect / Change device buttons |
| Color wheel | Hue-saturation disc — drag or tap to pick color |
| Recent colors | Row of 5 last-used colors, tap to recall |
| Color readout | Hex + RGB display, power toggle, TX indicator |

## Connection States

`Disconnected` → `Connecting` → `Connected` → `Reconnecting` (on drop)

Auto-reconnects once on launch and once on unexpected disconnect.

## Build & Run

1. Pair your Android device with the HC-06 module via system Bluetooth settings.
2. Open the project in Android Studio and run on a physical device (Bluetooth is not available on emulators).
3. Tap **Connect**, select the HC-06 from the bonded-device picker, and start picking colors.

> Requires Android with Bluetooth Classic support. Minimum SDK: as configured in `app/build.gradle.kts`.
