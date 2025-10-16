package com.jonas.cbproto
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

object FallbackManager {
    var peerIp: String = ""
    var peerPort: Int = 8000
    fun send(payload: ByteArray) {
        val ip = peerIp
        val port = peerPort
        if (ip.isBlank()) return
        thread {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(ip, port), 1500)
                    val out: OutputStream = s.getOutputStream()
                    val framed = PacketManager.frame(payload)
                    out.write(framed)
                    out.flush()
                }
            } catch (e: Exception) {
                // ignore connection errors for prototype
            }
        }
    }
}
