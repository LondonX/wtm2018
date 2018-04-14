package com.londonx.wtm2018

import android.os.SystemClock
import com.google.android.things.pio.PeripheralManager
import com.leinardi.android.things.driver.hd44780.Hd44780
import java.io.IOException


/**
 * Created by London on 2018/3/16.
 * LCD device
 */
const val LCD_ADDRESS = 0x3E

class LCDDevice(service: PeripheralManager) {
    var text = ""
        set(value) {
            field = value
            device.setBacklight(false)
            device.clearDisplay()
            device.cursorHome()
            device.setText(field)
        }


    private val device = service.let {
        val i2cName = service.i2cBusList.firstOrNull()
        Hd44780(i2cName, LCD_ADDRESS, Hd44780.Geometry.LCD_16X2)
                .also { showText() }
    }

    private fun showText() {
        Thread(Runnable {
            try {
                while (true) {
                    device.setBacklight(true)
                    device.cursorHome()
                    device.clearDisplay()
                    device.setText("Hello LCD")
                    val heart = intArrayOf(0, 10, 31, 31, 31, 14, 4, 0)
                    device.createCustomChar(heart, 0)
                    device.setCursor(10, 0)
                    device.writeCustomChar(0) // write :heart: custom character previously stored in location 0
                    delay(2)

                    device.clearDisplay()
                    device.setText("Backlight Off")
                    device.setBacklight(false)
                    delay(2)
                    device.clearDisplay()
                    device.setText("Backlight On")
                    device.setBacklight(true)
                    delay(2)

                    device.clearDisplay()
                    device.setText("Cursor On")
                    device.setCursorOn(true)
                    delay(2)

                    device.clearDisplay()
                    device.setText("Cursor Blink")
                    device.setBlinkOn(true)
                    delay(2)

                    device.clearDisplay()
                    device.setText("Cursor OFF")
                    device.setBlinkOn(false)
                    device.setCursorOn(false)
                    delay(2)

                    device.clearDisplay()
                    device.setText("Display Off")
                    device.setDisplayOn(false)
                    delay(2)

                    device.clearDisplay()
                    device.setText("Display On")
                    device.setDisplayOn(true)
                    delay(2)

                    device.clearDisplay()
                    for (i in 0 until 4) {
                        device.setCursor(0, i)
                        device.setText("-+* line $i *+-")
                    }
                    delay(2)

                    device.scrollDisplayLeft()
                    delay(2)

                    device.scrollDisplayLeft()
                    delay(2)

                    device.scrollDisplayLeft()
                    delay(2)

                    device.scrollDisplayRight()
                    delay(2)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun delay(s: Long) {
        SystemClock.sleep(s * 1000)
    }

    fun close() {
        device.close()
    }
}