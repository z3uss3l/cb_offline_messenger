package com.jonas.cbproto
import java.nio.ByteBuffer
object PacketManager {
    fun frame(payload: ByteArray): ByteArray {
        val len = payload.size
        val bb = ByteBuffer.allocate(1 + 2 + len + 2)
        bb.put(0x7E.toByte())
        bb.putShort(len.toShort())
        bb.put(payload)
        val crc = crc16(payload)
        bb.putShort(crc.toShort())
        return bb.array()
    }
    fun parse(frame: ByteArray): ByteArray? {
        if (frame.isEmpty() || frame[0] != 0x7E.toByte()) return null
        val len = ((frame[1].toInt() and 0xFF) shl 8) or (frame[2].toInt() and 0xFF)
        if (frame.size < 1+2+len+2) return null
        val payload = frame.copyOfRange(3, 3+len)
        val crc = ((frame[3+len].toInt() and 0xFF) shl 8) or (frame[3+len+1].toInt() and 0xFF)
        if (crc != crc16(payload)) return null
        return payload
    }
    private fun crc16(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (i in 0 until 8) {
                val odd = crc and 1 != 0
                crc = crc shr 1
                if (odd) crc = crc xor 0xA001
            }
        }
        return crc and 0xFFFF
    }
}
