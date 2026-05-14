package com.xlythe.compose.clock

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xlythe.view.clock.ClockHandView
import com.xlythe.view.clock.ClockView
import com.xlythe.view.clock.R

private const val TAG = "Clock"

enum class ClockStyle {
    ANALOG,
    DIGITAL
}

interface ClockController {
    fun start()
    fun stop()
    fun resetTime()
    fun setTime(timeInMillis: Long)
    fun setTime(hour: Int, minute: Int)
    fun setTime(hour: Int, minute: Int, second: Int)
    fun getTimeMillis(): Long
    fun getHour(): Int
    fun getMinute(): Int
    fun getSecond(): Int
}

internal class ClockControllerImpl(
    private val viewProvider: () -> ClockView?
) : ClockController {
    private fun getView(): ClockView? = viewProvider()

    override fun start() {
        getView()?.start() ?: Log.e(TAG, "View not available for start")
    }

    override fun stop() {
        getView()?.stop() ?: Log.e(TAG, "View not available for stop")
    }

    override fun resetTime() {
        getView()?.resetTime() ?: Log.e(TAG, "View not available for resetTime")
    }

    override fun setTime(timeInMillis: Long) {
        getView()?.setTime(timeInMillis) ?: Log.e(TAG, "View not available for setTime")
    }

    override fun setTime(hour: Int, minute: Int) {
        getView()?.setTime(hour, minute) ?: Log.e(TAG, "View not available for setTime")
    }

    override fun setTime(hour: Int, minute: Int, second: Int) {
        getView()?.setTime(hour, minute, second) ?: Log.e(TAG, "View not available for setTime")
    }

    override fun getTimeMillis(): Long {
        return getView()?.timeMillis ?: System.currentTimeMillis()
    }

    override fun getHour(): Int {
        return getView()?.hour ?: 0
    }

    override fun getMinute(): Int {
        return getView()?.minute ?: 0
    }

    override fun getSecond(): Int {
        return getView()?.second ?: 0
    }
}

/**
 * Clock is a Composable that simplifies the ClockView API without requiring XML layouts.
 *
 * @param modifier Modifier for layout.
 * @param clockStyle The style of the clock (analog or digital).
 * @param clockFaceRes Optional drawable resource for the background clock face / ticks.
 * @param hourHandRes Optional drawable resource for the hour hand.
 * @param minuteHandRes Optional drawable resource for the minute hand.
 * @param secondHandRes Optional drawable resource for the second hand.
 * @param digitalTextColor Color of the digital time text.
 * @param digitalTextSizeSp Text size in SP for the digital time text.
 * @param showSeconds Whether to show seconds.
 * @param showMilliseconds Whether to show milliseconds.
 * @param partialRotation Whether partial rotation is enabled.
 * @param lowBitAmbient Whether low bit ambient mode is enabled.
 * @param hasBurnInProtection Whether burn in protection is enabled.
 * @param ambientModeEnabled Whether ambient mode is enabled.
 * @param controller Controller to interact with the ClockView imperatively.
 * @param onTimeTick Callback invoked when the time ticks.
 */
@Composable
fun Clock(
    modifier: Modifier = Modifier,
    clockStyle: ClockStyle = ClockStyle.ANALOG,
    @DrawableRes clockFaceRes: Int? = null,
    @DrawableRes hourHandRes: Int? = null,
    @DrawableRes minuteHandRes: Int? = null,
    @DrawableRes secondHandRes: Int? = null,
    digitalTextColor: Color = Color.White,
    digitalTextSizeSp: Float = 40f,
    showSeconds: Boolean = true,
    showMilliseconds: Boolean = false,
    partialRotation: Boolean = false,
    lowBitAmbient: Boolean = false,
    hasBurnInProtection: Boolean = false,
    ambientModeEnabled: Boolean = false,
    controller: MutableState<ClockController?>? = null,
    onTimeTick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize and cache our ClockView programmatically.
    val clockView = remember(clockFaceRes, hourHandRes, minuteHandRes, secondHandRes) {
        val view = object : ClockView(context) {
            fun initializeProgrammatically() {
                // 1. Clock Face / Ticks Background
                if (clockFaceRes != null) {
                    val faceView = ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        setImageResource(clockFaceRes)
                    }
                    addView(faceView)
                }

                // 2. Digital Time TextView
                val timeView = TextView(context).apply {
                    id = R.id.clock_time
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                    setTextColor(digitalTextColor.toArgb())
                    textSize = digitalTextSizeSp
                }
                addView(timeView)

                // 3. Hour Hand
                if (hourHandRes != null) {
                    val hourView = ClockHandView(context).apply {
                        id = R.id.clock_hours
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
                        setImageResource(hourHandRes)
                    }
                    addView(hourView)
                }

                // 4. Minute Hand
                if (minuteHandRes != null) {
                    val minuteView = ClockHandView(context).apply {
                        id = R.id.clock_minutes
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
                        setImageResource(minuteHandRes)
                    }
                    addView(minuteView)
                }

                // 5. Second Hand
                if (secondHandRes != null) {
                    val secondView = ClockHandView(context).apply {
                        id = R.id.clock_seconds
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL)
                        setImageResource(secondHandRes)
                    }
                    addView(secondView)
                }

                // Trigger internal view binding (mTimeView, mHours, mMinutes, mSeconds)
                onFinishInflate()
            }
        }

        view.initializeProgrammatically()
        view.setOnTimeTickListener { onTimeTick() }
        view
    }

    // Cache our clock controller.
    val clockController = remember(clockView) { ClockControllerImpl { clockView } }
    DisposableEffect(clockController) {
        controller?.value = clockController
        onDispose {}
    }

    // Monitor lifecycle states.
    DisposableEffect(lifecycleOwner, clockView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> clockView.start()
                Lifecycle.Event.ON_STOP -> clockView.stop()
                Lifecycle.Event.ON_DESTROY -> clockView.stop()
                else -> { /* Do nothing */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val sparseArray = SparseArray<Parcelable>()
            clockView.saveHierarchyState(sparseArray)
            clockView.stop()
            if (controller?.value === clockController) {
                controller?.value = null
            }
            clockView.setOnTimeTickListener(null)
        }
    }

    // Wrap our View in a Composable, and keep it up to date.
    AndroidView(factory = { _ -> clockView }, update = { view ->
        view.setDigitalEnabled(clockStyle == ClockStyle.DIGITAL)
        view.setSecondsEnabled(showSeconds)
        view.setMillisecondsEnabled(showMilliseconds)
        view.setPartialRotationEnabled(partialRotation)
        view.setLowBitAmbient(lowBitAmbient)
        view.setHasBurnInProtection(hasBurnInProtection)
        view.setAmbientModeEnabled(ambientModeEnabled)
    }, modifier = modifier)
}
