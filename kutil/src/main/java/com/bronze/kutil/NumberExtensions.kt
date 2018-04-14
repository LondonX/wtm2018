package com.bronze.kutil

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by London on 2017/12/15.
 * extensions for operations of numbers
 */
fun Int.toBytes(order: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray =
        ByteBuffer.allocate(4).putInt(this).order(order).array()

fun Short.toBytes(order: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray =
        ByteBuffer.allocate(2).putShort(this).order(order).array()

fun ByteArray.toIntOrNull(order: ByteOrder = ByteOrder.BIG_ENDIAN): Int? {
    return try {
        this.toInt(order)
    } catch (ignore: IllegalArgumentException) {
        null
    }
}

fun ByteArray.toInt(order: ByteOrder = ByteOrder.BIG_ENDIAN) =
        when (this.size) {
            2 -> ByteBuffer.wrap(this).order(order).short.toInt()
            4 -> ByteBuffer.wrap(this).order(order).int
            8 -> ByteBuffer.wrap(this).order(order).long.toInt()
            else -> throw IllegalArgumentException("ByteArray(size: ${this.size}) is not valid for toInt")
        }


fun Int.getUnsignedLong() = this.toLong() and 0x00000000ffffffffL
