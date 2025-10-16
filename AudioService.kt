package com.jonas.cbproto
import android.app.Service
import android.content.Intent
import android.media.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors

class AudioService : Service() {
    private val encoder = ModemEncoder()
    private val decoder = ModemDecoder()
    private val exec = Executors.newSingleThreadExecutor()

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private val bufferSize = AudioRecord.getMinBufferSize(ModemConstants.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

    private var lastActivityBroadcast = 0L
    companion object {
        const val ACTION_CHANNEL = "com.jonas.cbproto.CHANNEL_UPDATE"
        const val EXTRA_CHANNEL = "channel"
        const val CHANNEL_MODEM = "MODEM"
        const val CHANNEL_FALLBACK = "FALLBACK"
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        setupAudio()
        startRecordingLoop()
        startChannelMonitor()
    }

    private fun startForegroundServiceNotification() {
        val chanId = "modem_service"
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(android.app.NotificationChannel(chanId, "Modem", android.app.NotificationManager.IMPORTANCE_LOW))
        }
        val n = NotificationCompat.Builder(this, chanId)
            .setContentTitle("CB Messenger Service").setContentText("running")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        startForeground(1, n)
    }

    private fun setupAudio() {
        audioTrack = AudioTrack(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(ModemConstants.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.play()
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, ModemConstants.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(bufferSize, ModemConstants.SAMPLES_PER_SYMBOL.toInt()*4))
        audioRecord?.startRecording()
    }

    private fun startRecordingLoop() {
        exec.execute {
            val readBuf = ShortArray(2048)
            while (!Thread.currentThread().isInterrupted) {
                val r = audioRecord?.read(readBuf, 0, readBuf.size) ?: 0
                if (r > 0) {
                    decoder.process(readBuf.copyOf(r)) { bytes ->
                        val payload = PacketManager.parse(bytes) ?: bytes
                        val i = Intent(ACTION_CHANNEL).apply {
                            putExtra(EXTRA_CHANNEL, CHANNEL_MODEM)
                            putExtra("payload", payload)
                        }
                        sendBroadcast(i)
                    }
                }
            }
        }
    }

    fun sendBytes(payload: ByteArray) {
        val now = System.currentTimeMillis()
        val modemActive = (now - decoder.lastReceiveTimestamp) < 5000
        if (modemActive) {
            exec.execute {
                val pcm = encoder.encodeByteStream(payload)
                audioTrack?.write(pcm, 0, pcm.size)
            }
            sendBroadcast(Intent(ACTION_CHANNEL).putExtra(EXTRA_CHANNEL, CHANNEL_MODEM))
        } else {
            FallbackManager.send(payload)
            sendBroadcast(Intent(ACTION_CHANNEL).putExtra(EXTRA_CHANNEL, CHANNEL_FALLBACK))
        }
    }

    private fun startChannelMonitor() {
        exec.execute {
            while (!Thread.currentThread().isInterrupted) {
                val now = System.currentTimeMillis()
                val modemActive = (now - decoder.lastReceiveTimestamp) < 5000
                val channel = if (modemActive) CHANNEL_MODEM else CHANNEL_FALLBACK
                sendBroadcast(Intent(ACTION_CHANNEL).putExtra(EXTRA_CHANNEL, channel))
                Thread.sleep(1500)
            }
        }
    }

    override fun onBind(intent: Intent?) = LocalBinder()
    inner class LocalBinder : Binder() { fun getService() = this@AudioService }
    override fun onDestroy() {
        audioRecord?.stop(); audioRecord?.release()
        audioTrack?.stop(); audioTrack?.release()
        exec.shutdownNow()
        super.onDestroy()
    }
}
