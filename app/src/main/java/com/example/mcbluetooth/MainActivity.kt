package com.example.mcbluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var btnConnect: Button

    private val LOCATION_PERMISSION_REQUEST = 1
    private val HC05_ADDRESS = "00:22:09:01:16:EA" // ← înlocuiește cu adresa MAC a modulului tău
    private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Disconnect vars
    private var bluetoothSocket: BluetoothSocket? = null
    private var isUserDisconnecting = false
    private lateinit var btnDisconnect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()

        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        btnConnect = findViewById(R.id.btnConnect)

        // Cerem permisiuni la start
        checkPermissions()

        btnConnect.setOnClickListener {
            if (checkPermissions()) {
                connectToHC05()
            }
        }

        btnDisconnect = findViewById(R.id.btnDisconnect)

        btnDisconnect.setOnClickListener {
            disconnectFromHC05()
        }
    }

    // Cererea permisiunilor runtime
    private fun checkPermissions(): Boolean {
        val fineLocation = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        val bluetoothConnect = checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        val bluetoothScan = checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN)

        return if (fineLocation != PackageManager.PERMISSION_GRANTED ||
            coarseLocation != PackageManager.PERMISSION_GRANTED ||
            bluetoothConnect != PackageManager.PERMISSION_GRANTED ||
            bluetoothScan != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ),
                LOCATION_PERMISSION_REQUEST
            )
            false
        } else true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // aici poți gestiona dacă permisiunile au fost acordate
    }

    // Conectarea la HC-05 și citirea datelor
    private fun connectToHC05() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        // Verificăm permisiunile (Android 12+)
        if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        val hc05Device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(HC05_ADDRESS)

        Thread {
            try {
                // Actualizăm UI-ul pentru a arăta că încercăm conectarea
                runOnUiThread {
                    btnConnect.text = "Se conectează..."
                    btnConnect.isEnabled = false
                }

                // Încercăm conexiunea standard
                var socket: BluetoothSocket? = hc05Device?.createRfcommSocketToServiceRecord(UUID_SPP)

                bluetoothAdapter?.cancelDiscovery()


                try {
                    socket?.connect()
                } catch (e: Exception) {
                    // Fallback: Dacă metoda standard eșuează, încercăm metoda via Reflection
                    socket = hc05Device?.javaClass?.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        ?.invoke(hc05Device, 1) as BluetoothSocket
                    socket.connect()
                }

                // Salvăm referința în variabila globală
                bluetoothSocket = socket

                // Dacă am ajuns aici, suntem conectați
                runOnUiThread {
                    btnConnect.text = "Conectat"
                    btnConnect.setBackgroundColor(Color.GREEN)
                    btnConnect.isEnabled = true
                    btnDisconnect.visibility = View.VISIBLE
                }

                // Folosim BufferedReader pentru a citi datele linie cu linie (\r\n de la STM32)
                val reader = BufferedReader(InputStreamReader(socket?.inputStream))

                while (true) {
                    val line = reader.readLine() // Așteaptă până primește un terminator de linie
                    if (line != null) {
                        runOnUiThread {
                            parseAndDisplay(line)
                        }
                    }
                }

            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread { btnConnect.text = "Permission Error" }
            } catch (e: Exception) {
                e.printStackTrace()

                // Verificăm dacă a fost o eroare reală sau o deconectare manuală
                if (!isUserDisconnecting) {
                    runOnUiThread {
                        btnConnect.text = "Connection Error"
                        btnConnect.setBackgroundColor(Color.RED)
                        btnConnect.isEnabled = true
                        btnDisconnect.visibility = View.GONE
                    }
                }
            } finally {
                // Resetăm flag-ul pentru următoarea încercare de conectare
                isUserDisconnecting = false
            }
        }.start()
    }

    private fun disconnectFromHC05() {
        try {
            isUserDisconnecting = true // Semnalizăm că noi am apăsat butonul
            bluetoothSocket?.close()
            bluetoothSocket = null

            runOnUiThread {
                btnConnect.text = "Conectează-te la HC-05"
                btnConnect.isEnabled = true
                btnConnect.setBackgroundColor(Color.BLUE) // Revenim la culoarea inițială
                btnDisconnect.visibility = View.GONE

                tvTemperature.text = "Temp: --°C"
                tvHumidity.text = "Hum: --%"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Parsarea datelor și afișarea în TextView
    private fun parseAndDisplay(data: String) {
        // Regex adaptat pentru formatul: "Temp: 25.5 C | Hum: 60.0 %"
        val tempRegex = "Temp: ([0-9.-]+)".toRegex()
        val humRegex = "Hum: ([0-9.-]+)".toRegex()

        try {
            tempRegex.find(data)?.groups?.get(1)?.value?.let {
                tvTemperature.text = "Temp: $it°C"
                // Opțional: Schimbă culoarea dacă e prea cald
                if (it.toFloat() > 30) tvTemperature.setTextColor(Color.RED)
                else tvTemperature.setTextColor(Color.GREEN)
            }

            humRegex.find(data)?.groups?.get(1)?.value?.let {
                tvHumidity.text = "Hum: $it%"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
