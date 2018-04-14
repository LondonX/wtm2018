package com.londonx.led

import android.app.Activity
import android.os.Bundle
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class LedActivity : Activity() {

    private val service = PeripheralManager.getInstance()
    private val pinR = service.openGpio("BCM17").also { it.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW) }
    private val pinG = service.openGpio("BCM27").also { it.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW) }
    private val pinB = service.openGpio("BCM22").also { it.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        while (!isDestroyed) {
            Thread.sleep(16)
        }
    }
}
