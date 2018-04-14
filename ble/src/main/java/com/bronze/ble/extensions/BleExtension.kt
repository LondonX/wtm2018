package com.bronze.ble.extensions

import com.bronze.ble.BleService
import com.bronze.kutil.loadPref
import com.bronze.kutil.savePref
import com.polidea.rxandroidble.RxBleDevice

/**
 * Created by London on 2017/12/23.
 * extensions for RxBle*
 */
var RxBleDevice.alias: String
    get() {
        if (BleService.instance == null) {
            return readableName
        }
        val a = BleService.instance?.loadPref(
                "alias_${macAddress.replace(":", "")}",
                "")
        return if (a.isNullOrBlank()) {
            readableName
        } else {
            a!!
        }
    }
    set(value) {
        if (BleService.instance == null) {
            return
        }
        BleService.instance?.savePref(
                "alias_${macAddress.replace(":", "")}",
                value)
    }

val RxBleDevice.readableName: String
    get() {
        if (name.isNullOrBlank()) {
            return macAddress ?: "UNKNOWN"
        }
        return name!!
    }