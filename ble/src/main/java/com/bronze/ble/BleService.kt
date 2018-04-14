package com.bronze.ble

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.util.Log
import com.bronze.ble.`object`.debugDevice
import com.bronze.ble.impl.BleListener
import com.bronze.kutil.*
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.startService
import rx.Subscription
import java.nio.channels.AlreadyConnectedException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * Created by London on 2017/12/11.
 * 蓝牙BLE服务
 * 0. add meta-data RX_UUID TX_UUID and SERVICE_UUID(OPTIONAL for scan filter) RX_MODE in app manifest
 * 1. instance start
 * 2. add/remove Listeners
 */
class BleService : Service() {
    companion object {
        var instance: BleService? = null
        private var started: () -> Unit = {}
        fun start(context: Context, started: () -> Unit = {}) {
            if (instance != null) {
                started.invoke()
                return
            }
            this.started = started
            context.startService<BleService>()
        }
    }

    val connectingDevices = HashSet<RxBleDevice>()
    val connectedDevices = ArrayList<RxBleDevice>()
    val nearDevices = ArrayList<RxBleDevice>()
    val lastConnectAddresses by lazy {
        loadPref("lastConnectAddresses", ArrayList<String>())
    }

    fun saveLastConnectAddresses() {
        savePref("lastConnectAddresses", lastConnectAddresses)
    }

    private var debug = true
    private var rxMode = ""//indicate or notification
    private var rxUUID = UUID.randomUUID()
    private var txUUID = UUID.randomUUID()
    var serviceUUID: UUID? = null

    private val bleListeners = HashSet<BleListener>()
    private val connectiveMap = HashMap<String, Subscription>()
    private val connectionMap = HashMap<String, RxBleConnection>()
    private var debugReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?) = Binder()
    override fun onCreate() {
        super.onCreate()
        instance = this

        val md = packageManager.getServiceInfo(ComponentName(this, javaClass),
                PackageManager.GET_META_DATA).metaData
        debug = (md?.get("DEBUG") as? Boolean) ?: true
        rxMode = (md?.get("RX_MODE") as? String) ?: "notification"
        try {
            rxUUID = UUID.fromString(md?.get("RX_UUID").toString())
            txUUID = UUID.fromString(md?.get("TX_UUID").toString())
        } catch (ignore: Exception) {
            throw NullPointerException("meta-data RX_UUID or TX_UUID not set correctly!!!")
        }
        serviceUUID = md?.get("SERVICE_UUID")
                .let {
                    if (it == null) {
                        null
                    } else {
                        UUID.fromString(it.toString())
                    }
                }

        started.invoke()
        if (debug) {
            debugReceiver = registerReceiver(packageName + ":ble") {
                onDebugDataReceived(it)
            }
        }
    }

    private fun onDebugDataReceived(intent: Intent) {
        val hex = intent.getStringExtra("hex")?.takeIf { it.isNotBlank() }
        if (hex == null) {
            Log.e("BleService", "debug receiver no hex(extra) received")
            return
        }
        val hexArray = hex.split(" ")
        val data = ByteArray(hexArray.size, {
            hexArray[it].toIntOrNull(16)?.toByte().let {
                if (it == null) {
                    Log.e("BleService", "debug receiver data error")
                    return
                } else {
                    it
                }
            }
        })
        log("debug received data: ${Arrays.toString(data)}")
        bleListeners.forEach { it.onDataReceived(debugDevice, data) }
    }

    override fun onDestroy() {
        super.onDestroy()
        safeUnregisterReceiver(debugReceiver)
    }

    fun connectMac(macAddress: String) {
        val device = findDeviceByMac(macAddress)
        if (device == null) {
            log("cannot create device by mac $macAddress")
            return
        }
        connect(device)
    }

    fun connect(rxBleDevice: RxBleDevice) {
        if (rxBleDevice.connectionState == RxBleConnection.RxBleConnectionState.CONNECTED ||
                rxBleDevice.connectionState == RxBleConnection.RxBleConnectionState.CONNECTING) {
            log("device(${rxBleDevice.macAddress}) state is ${rxBleDevice.connectionState}, " +
                    "will not be connect this at time")
            return
        }
        if (connectedDevices.contains(rxBleDevice)) {
            log("device(${rxBleDevice.macAddress}) is connected, will not be connect this at time")
            return
        }
        if (connectingDevices.contains(rxBleDevice)) {
            log("device(${rxBleDevice.macAddress}) is connecting, will not be connect this at time")
            return
        }
        connectingDevices.add(rxBleDevice)
        bleListeners.forEach { it.onDeviceConnectStarted(rxBleDevice) }
        val connectionSub = rxBleDevice.establishConnection(false)
                .subscribe({
                    connectionMap[rxBleDevice.macAddress] = it
                    val rxObs = if (rxMode == "notification") {
                        it.setupNotification(rxUUID)
                    } else {
                        it.setupIndication(rxUUID)
                    }
                    rxObs.flatMap {
                        connectedDevices.add(rxBleDevice)
                        nearDevices.remove(rxBleDevice)
                        runOnUiThread {
                            lastConnectAddresses.add(rxBleDevice.macAddress)
                            saveLastConnectAddresses()
                            connectingDevices.remove(rxBleDevice)
                            runOnUiThread {
                                bleListeners.forEach { it.onDeviceConnected(rxBleDevice) }
                            }
                        }
                        it
                    }.subscribe({ data ->
                        if (debug) {
                            log("<-${rxBleDevice.macAddress}:${Arrays.toString(data)}")
                        }
                        runOnUiThread {
                            bleListeners.forEach { it.onDataReceived(rxBleDevice, data) }
                        }
                    }, {
                        Log.e("BleService", "notification/indicate failed!!!", it)
                        disconnect(rxBleDevice, true)
                    })
                }, {
                    if (it is AlreadyConnectedException) {//ignoring err
                        connectingDevices.remove(rxBleDevice)
                        runOnUiThread {
                            bleListeners.forEach { it.onDeviceConnected(rxBleDevice) }
                        }
                        return@subscribe
                    }
                    Log.e("BleService", "connect failed!!!", it)
                    disconnect(rxBleDevice, true)
                })
        connectiveMap[rxBleDevice.macAddress] = connectionSub
    }

    fun disconnectMac(macAddress: String, withCallback: Boolean = false) {
        val device = findDeviceByMac(macAddress)
        if (device == null) {
            log("cannot create device by mac $macAddress")
            return
        }
        disconnect(device, withCallback)
    }

    fun disconnect(rxBleDevice: RxBleDevice, withCallback: Boolean = false) {
        connectiveMap[rxBleDevice.macAddress]?.unsubscribe()
        connectiveMap.remove(rxBleDevice.macAddress)
        connectingDevices.remove(rxBleDevice)
        connectedDevices.remove(rxBleDevice)
        if (withCallback) {
            runOnUiThread {
                bleListeners.forEach { it.onDeviceDisconnected(rxBleDevice) }
            }
        }
    }

    fun findDeviceByMac(macAddress: String): RxBleDevice? = RxBleClient.create(this).getBleDevice(macAddress)

    /**
     * write data to device, NULL is write to all connectedDevices
     */
    fun write(data: ByteArray, toMac: String? = null, done: () -> Unit = {}) {
        connectedDevices.forEach { device ->
            if (toMac == null || device.macAddress == toMac) {
                connectionMap[device.macAddress]
                        ?.writeCharacteristic(txUUID, data)
                        ?.subscribe {
                            if (debug) {
                                log("->${device.macAddress}:${Arrays.toString(data)}")
                            }
                            done.invoke()
                        }
            }
        }
    }

    fun addListener(bleListener: BleListener) {
        bleListeners.add(bleListener)
        if (connectingDevices.isNotEmpty()) {
            runOnUiThread {
                connectingDevices.forEach {
                    bleListener.onDeviceConnectStarted(it)
                }
            }
        }
    }

    fun removeListener(bleListener: BleListener) {
        bleListeners.remove(bleListener)
    }
}