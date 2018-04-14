package com.londonx.wtm2018

import android.app.Activity
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.os.Bundle
import com.bronze.kutil.registerReceiver
import com.bronze.kutil.safeUnregisterReceiver
import com.bronze.kutil.toBytes
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.londonx.thingsdata.*
import com.polidea.androidthings.driver.steppermotor.Direction
import com.polidea.androidthings.driver.uln2003.driver.ULN2003Resolution
import com.polidea.androidthings.driver.uln2003.motor.ULN2003StepperMotor
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.bluetoothManager
import org.jetbrains.anko.wifiManager
import java.util.*
import kotlin.concurrent.schedule

class ThingsActivity : Activity() {

    private val peripheral = PeripheralManager.getInstance()
    private val ledR = peripheral.openGpio("BCM18").also { it.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW) }
    private val ledG = peripheral.openGpio("BCM23").also { it.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW) }
    private val ledB = peripheral.openGpio("BCM24").also { it.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW) }
    private val motor = ULN2003StepperMotor("BCM4", "BCM17", "BCM27", "BCM22")

    private val bleServer by lazy {
        BLEServer(this,
                onConnectionStateChange = { _, _, newState ->
                    runOnUiThread {
                        tvBtStatus.text =
                                "BT: ${if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    isConnected = true
                                    "CONNECTED"
                                } else {
                                    isConnected = false
                                    "NOT CONNECTED"
                                }}"
                    }
                },
                onDataChanged = {
                    ledG.value = true
                    runOnUiThread {
                        ledB.value = true
                        tvBtData.text = "Data:\n${it.joinToString { "%x".format(it) }}"
                        handlerData(it)
                        ledB.value = false
                    }
                    ledG.value = false
                })
    }
    private var isConnected = false

    private var connectiveChangeReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothManager.adapter.enable()
        bleServer.startAdvertising()
        ledR.value = true
        Timer().schedule(0, 1000) {
            if (isConnected) {
                ledR.value = false
            } else {
                ledR.value = !ledR.value
            }
        }

        connectiveChangeReceiver =
                registerReceiver("android.net.conn.CONNECTIVITY_CHANGE") {
                    printWifiStatus()
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleServer.destroy()
        safeUnregisterReceiver(connectiveChangeReceiver)
    }

    private fun printWifiStatus() {
        wifiManager.connectionInfo.also {
            if (it == null) {
                tvWifi.text = "WiFi: no connectionInfo"
                return@also
            }
            tvWifi.text = "WiFi: ${it.ssid} ${intToIp(it.ipAddress)}"
        }
    }

    private fun handlerData(data: ByteArray) {
        if (!isDataValid(data)) {
            return
        }
        when (data[1]) {
            CMD_GET_IP -> {
                bleServer.sendData(assembleData(CMD_GET_IP,
                        *wifiManager.connectionInfo.let {
                            (it?.ipAddress ?: 0).toBytes()
                        }))
            }
            CMD_RUN -> motor.rotate(36000.0, Direction.CLOCKWISE, ULN2003Resolution.HALF.id, 12.0)
            CMD_STOP -> {
                motor.cancelCurrentRotation()
                motor.lowAllGpio()
            }
        }
    }
}
