package com.jonas.cbproto
import android.app.Activity
import android.content.*
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat

class MessengerActivity : Activity() {
    private var service: AudioService? = null
    private val conn = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, ib: IBinder?) {
            val binder = ib as AudioService.LocalBinder
            service = binder.getService()
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null }
    }

    private val chanReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ch = intent?.getStringExtra(AudioService.EXTRA_CHANNEL) ?: "UNKNOWN"
            val tv = findViewById<TextView>(R.id.channelIndicator)
            tv.text = "Channel: $ch"
            val payload = intent?.getByteArrayExtra("payload")
            if (payload != null) {
                val container = findViewById<LinearLayout>(R.id.msgContainer)
                val t = TextView(this@MessengerActivity)
                t.text = "RX: ${String(payload)}"
                container.addView(t)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)
        bindService(Intent(this, AudioService::class.java), conn, Context.BIND_AUTO_CREATE)
        startService(Intent(this, AudioService::class.java))
        registerReceiver(chanReceiver, IntentFilter(AudioService.ACTION_CHANNEL))

        val et = findViewById<EditText>(R.id.inputText)
        val btn = findViewById<Button>(R.id.sendBtn)
        btn.setOnClickListener {
            val msg = et.text.toString().toByteArray(Charsets.UTF_8)
            val framed = PacketManager.frame(msg)
            service?.sendBytes(framed)
            val container = findViewById<LinearLayout>(R.id.msgContainer)
            val t = TextView(this@MessengerActivity)
            t.text = "TX: ${String(msg)}"
            container.addView(t)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chanReceiver)
        unbindService(conn)
    }
}
