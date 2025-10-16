package com.jonas.cbproto
import kotlin.math.*
class ModemEncoder {
    private val sr = ModemConstants.SAMPLE_RATE
    private val samplesPerSym = ModemConstants.SAMPLES_PER_SYMBOL.toInt()
    private val twoPi = 2.0 * Math.PI
    fun encodeByteStream(data: ByteArray): ShortArray {
        val samples = ArrayList<Short>()
        for (b in data) {
            appendTone(samples, ModemConstants.FREQ_SPACE, samplesPerSym)
            var v = b.toInt() and 0xFF
            for (i in 0 until 8) {
                val bit = (v shr i) and 1
                appendTone(samples, if (bit == 1) ModemConstants.FREQ_MARK else ModemConstants.FREQ_SPACE, samplesPerSym)
            }
            appendTone(samples, ModemConstants.FREQ_MARK, samplesPerSym)
        }
        return samples.toShortArray()
    }
    private fun appendTone(buf: MutableList<Short>, freq: Double, len: Int) {
        val phaseInc = twoPi * freq / sr
        var phase = 0.0
        for (i in 0 until len) {
            val s = (sin(phase) * Short.MAX_VALUE * 0.6).toShort()
            buf.add(s)
            phase += phaseInc
            if (phase > twoPi) phase -= twoPi
        }
    }
}
