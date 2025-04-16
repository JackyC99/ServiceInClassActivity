package edu.temple.myapplication

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    val timerHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            findViewById<TextView>(R.id.textView).text = msg.what.toString()
            super.handleMessage(msg)
        }
    }

    lateinit var timerBinder: TimerService.TimerBinder
    var isConnected = false

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            timerBinder.setHandler(timerHandler)
            isConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindService(
            Intent(this, TimerService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (isConnected) {
                // Retrieve the last saved count or use a default value of 100
                val startValue = getSharedPreferences("TimerPrefs", MODE_PRIVATE).getInt("count", 100)
                timerBinder.start(startValue)
            }
        }


        findViewById<Button>(R.id.stopButton).setOnClickListener {
            if (isConnected) {
                timerBinder.stop()
            }
        }
    }

    override fun onDestroy() {
        if (isConnected) unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.start_pause -> {
                if (isConnected) {
                    if (!timerBinder.isRunning) {
                        val startValue = getSharedPreferences("TimerPrefs", MODE_PRIVATE).getInt("count", 100)
                        timerBinder.start(startValue)
                        item.title = "Pause"
                        item.setIcon(android.R.drawable.ic_media_pause)
                    } else {
                        timerBinder.pause()
                        item.title = "Start"
                        item.setIcon(android.R.drawable.ic_media_play)
                    }
                }
                return true
            }

            R.id.stop -> {
                if (isConnected) {
                    timerBinder.stop()
                }
                return true
            }

            else -> return false
        }
        return super.onOptionsItemSelected(item)
    }
}
