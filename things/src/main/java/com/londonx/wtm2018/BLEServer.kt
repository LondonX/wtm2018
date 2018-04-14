package com.londonx.wtm2018

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import org.jetbrains.anko.bluetoothManager
import java.util.*

/**
 * Created by London on 2018/3/19.
 * BLE server
 */
class BLEServer(private val context: Context,
                onConnectionStateChange: (device: BluetoothDevice?, status: Int, newState: Int) -> Unit,
                private val onDataChanged: (data: ByteArray) -> Unit) {

    //BLE server
    private val server by lazy { context.bluetoothManager.openGattServer(context, serverCallback) }

    //services & characteristics & descriptors
    private val service = BluetoothGattService(
            UUID.fromString("00000000-0000-0000-0000-1234567890AB"),
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
    private val rtCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString("00000001-0000-0000-0000-1234567890AB"),
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE or
                    BluetoothGattCharacteristic.PERMISSION_READ
    )
    private val notificationDescriptor = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_WRITE
    )

    //callbacks
    private val serverCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            onConnectionStateChange.invoke(device, status, newState)
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                startAdvertising()
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            if (responseNeeded) {
                server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
            }
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            if (responseNeeded) {
                server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
            }
            value ?: return
            onDataChanged.invoke(value)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf())
        }
    }
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("ThingsAct", "AdvertiseCallback.onStartFailure(errorCode: $errorCode)")
        }
    }

    init {
        rtCharacteristic.addDescriptor(notificationDescriptor)
        service.addCharacteristic(rtCharacteristic)
        server.addService(service)
    }

    fun startAdvertising() {
        stopAdvertising()
        context.bluetoothManager.adapter.bluetoothLeAdvertiser
                .startAdvertising(AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setConnectable(true)
                        .build(),
                        AdvertiseData.Builder()
                                .setIncludeDeviceName(true)
                                .addServiceUuid(ParcelUuid(service.uuid))
                                .build(), advertiseCallback
                )
    }

    private fun stopAdvertising() {
        context.bluetoothManager.adapter.bluetoothLeAdvertiser
                .stopAdvertising(advertiseCallback)
    }

    fun destroy() {
        stopAdvertising()
        server.clearServices()
    }

    fun sendData(data: ByteArray) {
        rtCharacteristic.value = data
        context.bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).forEach {
            server.notifyCharacteristicChanged(it, rtCharacteristic, false)
        }
    }
}