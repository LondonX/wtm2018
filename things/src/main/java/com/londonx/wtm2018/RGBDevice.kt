package com.londonx.wtm2018

import com.google.android.things.pio.PeripheralManager

/**
 * Created by London on 2018/3/16.
 * rgb Device
 */

const val RGB_ADDRESS = 0x62

const val REG_RED = 0x04
const val REG_GREEN = 0x03
const val REG_BLUE = 0x02
const val REG_MODE1 = 0x00
const val REG_MODE2 = 0x01
const val REG_OUTPUT = 0x08

class RGBDevice(service: PeripheralManager) {
    private val device = service.let {
        val i2cName = service.i2cBusList.firstOrNull()
        val dev = service.openI2cDevice(i2cName, RGB_ADDRESS)
        dev.writeRegByte(REG_MODE1, 0)
        dev.writeRegByte(REG_OUTPUT, 0xFF.toByte())
        dev
    }

    fun setColor(r: Int, g: Int, b: Int) {
        device.writeRegByte(REG_RED, r.toByte())
        device.writeRegByte(REG_GREEN, g.toByte())
        device.writeRegByte(REG_BLUE, b.toByte())
    }

    fun close() {
        device.close()
    }
}