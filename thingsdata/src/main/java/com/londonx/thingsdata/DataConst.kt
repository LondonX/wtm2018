package com.londonx.thingsdata

/**
 * Created by London on 2018/3/23.
 * android things data assembler
 */
const val HEADER = 'L'.toByte()
const val FOOTER = 'X'.toByte()
const val CMD_GET_IP = 1.toByte()
const val CMD_RUN = 2.toByte()
const val CMD_STOP = 3.toByte()

fun assembleData(cmd: Byte, vararg data: Byte) = byteArrayOf(HEADER, cmd, *data, FOOTER)

fun isDataValid(data: ByteArray) =
        data.firstOrNull() == HEADER && data.lastOrNull() == FOOTER && data.size > 2

fun intToIp(ip: Int): String {
    var ipCalc = ip
    val result = StringBuilder(15)

    for (i in 0..3) {
        result.append(ipCalc and 0xff)
        if (i < 3) {
            result.append('.')
        }
        ipCalc = ipCalc shr 8
    }
    return result.toString()
}