# 📱 Bluetooth Environmental Monitor (Android Client)

*This repository contains the source code for the Android application used to monitor environmental data (temperature and humidity) sent from an STM32F429I-Discovery microcontroller via the HC-05 Bluetooth module.*

## ✨ Features

RFCOMM Bluetooth Connection: *Establishes a secure serial link using the SPP (Serial Port Profile).*

Real-time Data Parsing: *Uses high-efficiency Regular Expressions (Regex) to extract values from the incoming data stream.*

Connection Resilience: *Implements a "Reflection" fallback method to ensure compatibility with a wide range of Android devices.*

Dynamic UI: *Features a Material Design interface that changes colors based on temperature thresholds (e.g., turns Red above 30°C).*

Intelligent Buffering: *Utilizes BufferedReader to process complete data lines ending in \r\n, preventing flickering or data fragmentation.*

## 🛠️ Technical Stack

Language: *Kotlin*

Platform: *Android (API Level 31+ support)*

UI Framework: *XML Layouts with Material Design components*

Threading: *Asynchronous background threads for Bluetooth communication to ensure UI fluidity.*

## 📂 Project Structure

MainActivity.kt: *Contains the logic for permission handling, Bluetooth socket management, and data parsing.*

activity_main.xml: *The user interface layout containing the Connect/Disconnect buttons and data displays.*

AndroidManifest.xml: *Configured with the necessary Bluetooth permissions (BLUETOOTH_CONNECT, BLUETOOTH_SCAN).*

## ⚙️ Configuration
Before building the APK, ensure you have set your module's MAC address in MainActivity.kt:
*private const val HC05_ADDRESS = "00:11:22:33:44:55" // Replace with your HC-05 MAC Address* 

## ⚠️ Requirements & Permissions

For the app to function on Android 12 and above, you must grant the following permissions when prompted:

Nearby Devices (Bluetooth Connect/Scan): Required to communicate with the HC-05 module.

## 🛠️ Troubleshooting

App Crashes on Connect: *Ensure the MAC address is valid and the "Nearby Devices" permission is granted manually in settings.*

Displays "0" Values: *Verify in Logcat (tag: BT_DATA) that the data format matches exactly: Temp: XX.X C | Hum: XX.X %.*

Connection Refused: *Ensure no other app (like Serial Bluetooth Terminal) is currently connected to the HC-05.*
