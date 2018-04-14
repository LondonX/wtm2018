package com.bronze.ble.impl

import com.polidea.rxandroidble.RxBleDevice

/**
 * Created by London on 2017/12/11.
 * BLE监听器
 */
interface BleListener {
    fun onDeviceConnectStarted(device: RxBleDevice) {}
    fun onDeviceConnected(device: RxBleDevice) {}
    fun onDeviceDisconnected(device: RxBleDevice) {}
    fun onDataReceived(device: RxBleDevice, data: ByteArray) {}
}