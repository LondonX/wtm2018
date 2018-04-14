package com.bronze.ble.`object`

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import rx.Observable

/**
 * Created by London on 2017/12/22.
 * debug device
 */
object debugDevice : RxBleDevice {
    override fun getMacAddress() = "12:34:56:78:90:AB"

    override fun getConnectionState() = RxBleConnection.RxBleConnectionState.CONNECTED

    override fun getName() = "DEBUG DEVICE"

    override fun observeConnectionStateChanges(): Observable<RxBleConnection.RxBleConnectionState> {
        throw NullPointerException("debug device has no Observable for observeConnectionStateChanges!!!")
    }

    @Deprecated("DO NOT USE")
    override fun establishConnection(context: Context?, autoConnect: Boolean): Observable<RxBleConnection> {
        throw NullPointerException("debug device has no Observable for establishConnection(deprecated)!!!")
    }

    override fun establishConnection(autoConnect: Boolean): Observable<RxBleConnection> {
        throw NullPointerException("debug device has no Observable for establishConnection!!!")
    }

    override fun getBluetoothDevice(): BluetoothDevice {
        throw NullPointerException("debug device has no BluetoothDevice for establishConnection!!!")
    }
}