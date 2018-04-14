package com.londonx.wtm2018

import android.content.BroadcastReceiver
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bronze.ble.BleService
import com.bronze.ble.impl.BleListener
import com.bronze.kutil.registerReceiver
import com.bronze.kutil.safeUnregisterReceiver
import com.bronze.kutil.toInt
import com.londonx.thingsdata.*
import com.londonx.weardata.ACTION_CLICK
import com.londonx.weardata.ACTION_DATA_RECEIVED
import com.londonx.weardata.EXTRA_DATA
import com.polidea.rxandroidble.RxBleDevice
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity(), BleListener {
    private var dataReceiver: BroadcastReceiver? = null
    private var isPlaying = false
        set(value) {
            field = value
            if (field) {
                lottie.resumeAnimation()
                BleService.instance?.write(assembleData(CMD_RUN))
            } else {
                lottie.pauseAnimation()
                BleService.instance?.write(assembleData(CMD_STOP))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgBluetooth.setOnClickListener { startActivity<BluetoothActivity>() }
        imgBluetooth.setOnLongClickListener {
            BleService.instance?.write(assembleData(CMD_GET_IP))
            true
        }
        lottie.setOnClickListener { isPlaying = !isPlaying }

        dataReceiver = registerReceiver(ACTION_DATA_RECEIVED) {
            val jo = try {
                JSONObject(it.getStringExtra(EXTRA_DATA))
            } catch (ignore: JSONException) {
                return@registerReceiver
            }
            val action = jo.getInt("a")
            if (action == ACTION_CLICK) {
                isPlaying = !isPlaying
            }
        }
    }

    override fun onResume() {
        super.onResume()

        BleService.start(this) {
            //无法重连，things每次广播的mac地址都不同
            BleService.instance?.addListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        safeUnregisterReceiver(dataReceiver)
        BleService.instance?.removeListener(this)
    }

    override fun onDeviceConnected(device: RxBleDevice) {
        super.onDeviceConnected(device)
        imgBluetooth.imageResource = R.drawable.ic_bluetooth_connected
    }

    override fun onDeviceDisconnected(device: RxBleDevice) {
        super.onDeviceDisconnected(device)
        imgBluetooth.imageResource = R.drawable.ic_bluetooth
    }

    override fun onDataReceived(device: RxBleDevice, data: ByteArray) {
        super.onDataReceived(device, data)
        handleData(data)
    }

    private fun handleData(data: ByteArray) {
        if (!isDataValid(data)) {
            return
        }
        when (data[1]) {
            CMD_GET_IP -> {
                val ip = data.copyOfRange(2, data.lastIndex).toInt()
                if (ip == 0) {
                    toast("deviceIP: no IP")
                } else {
                    toast("deviceIP: ${intToIp(ip)}")
                }
            }
        }
    }
}
