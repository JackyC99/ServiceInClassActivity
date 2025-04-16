package edu.temple.myapplication

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log

class TimerService : Service() {

    private var isRunning = false
    private var paused = false
    private var timerHandler: Handler? = null

    lateinit var prefs: SharedPreferences
    private val PREF_NAME = "TimerPrefs"
    private val KEY_COUNT = "count"
    private val KEY_PAUSED = "paused"

    lateinit var t: TimerThread

    inner class TimerBinder : Binder() {
        val isRunning: Boolean
            get() = this@TimerService.isRunning

        val paused: Boolean
            get() = this@TimerService.paused

        fun start(defaultValue: Int) {
            // Retrieve saved value if paused, else use defaultValue
            val resumeFrom = if (prefs.getBoolean(KEY_PAUSED, false)) {
                prefs.getInt(KEY_COUNT, defaultValue)
            } else {
                defaultValue
            }

            if (!paused) {
                if (!isRunning) {
                    if (::t.isInitialized) t.interrupt()
                    this@TimerService.start(resumeFrom)
                }
            } else {
                pause()
            }
        }

        fun setHandler(handler: Handler) {
            timerHandler = handler
        }

        fun stop() {
            if (::t.isInitialized || isRunning) {
                t.interrupt()
                prefs.edit().clear().apply()  // Clear state on stop
            }
        }

        fun pause() {
            this@TimerService.pause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        Log.d("TimerService status", "Created")
    }

    override fun onBind(intent: Intent): IBinder {
        return TimerBinder()
    }

    fun start(startValue: Int) {
        t = TimerThread(startValue)
        t.start()
    }

    fun pause() {
        if (::t.isInitialized) {
            paused = !paused
            isRunning = !paused
            saveState()  // Save state when paused
        }
    }

    inner class TimerThread(private val startValue: Int) : Thread() {
        var currentCount = startValue  // Start from the provided start value

        override fun run() {
            isRunning = true
            try {
                for (i in currentCount downTo 1) { // Use currentCount instead of startValue
                    if (Thread.interrupted()) break
                    while (paused);
                    currentCount = i
                    Log.d("Countdown", i.toString())
                    timerHandler?.sendEmptyMessage(i)
                    sleep(1000)
                }
                isRunning = false
                paused = false
                saveState()  // Save state when the timer finishes
            } catch (e: InterruptedException) {
                Log.d("Timer interrupted", e.toString())
                isRunning = false
                paused = false
            }
        }
    }

    private fun saveState() {
        val editor = prefs.edit()

        // If the timer is running (and not paused), reset to default value
        if (isRunning) {
            editor.putInt(KEY_COUNT, 100)  // Set to default value
        } else {
            editor.putBoolean(KEY_PAUSED, paused)
            if (paused && ::t.isInitialized) {
                editor.putInt(KEY_COUNT, t.currentCount)  // Save the current count when paused
            } else {
                editor.putInt(KEY_COUNT, 0)  // Reset the count when finished
            }
        }

        editor.apply()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        saveState()  // Ensure state is saved when the service is unbound
        if (::t.isInitialized) t.interrupt()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        // If the timer is still running, reset to default value (100)
        if (isRunning) {
            saveState()  // Set to default value if running
        }
        super.onDestroy()
        Log.d("TimerService status", "Destroyed")
    }
}
