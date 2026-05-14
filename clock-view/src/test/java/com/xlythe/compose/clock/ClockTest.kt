package com.xlythe.compose.clock

import android.content.Context
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xlythe.view.clock.ClockHandView
import com.xlythe.view.clock.ClockView
import com.xlythe.view.clock.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ClockTest {

    @Test
    fun testClockController() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clockView = object : ClockView(context) {
            fun initForTest() {
                val timeView = TextView(context).apply { id = R.id.clock_time }
                addView(timeView)
                val hourView = ClockHandView(context).apply { id = R.id.clock_hours }
                addView(hourView)
                onFinishInflate()
            }
        }.apply { initForTest() }

        val controller: ClockController = ClockControllerImpl { clockView }
        assertNotNull(controller)

        // Test imperative start/stop
        controller.start()
        controller.stop()

        // Test imperative setTime
        controller.setTime(10, 30, 15)
        assertEquals(10, controller.getHour())
        assertEquals(30, controller.getMinute())
        assertEquals(15, controller.getSecond())

        // Test resetTime
        controller.resetTime()
        assertTrue(controller.getTimeMillis() <= System.currentTimeMillis())
    }
}
