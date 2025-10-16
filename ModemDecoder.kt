package com.jonas.cbproto
class ModemDecoder {
    private val block = ModemConstants.SAMPLES_PER_SYMBOL.toInt()
    private val detMark = GoertzelDetector(ModemConstants.FREQ_MARK, ModemConstants.SAMPLE_RATE, block)
    private val detSpace = GoertzelDetector(ModemConstants.FREQ_SPACE, ModemConstants.SAMPLE_RATE, block)
    var lastReceiveTimestamp: Long = 0
    fun process(samples: ShortArray, onPacketBytes: (ByteArray) -> Unit) {
        val bits = ArrayList<Int>()
        var i = 0
        while (i + block <= samples.size) {
            val pm = detMark.detect(samples, i)
            val ps = detSpace.detect(samples, i)
            val bit = if (pm > ps) 1 else 0
            bits.add(bit)
            i += block
        }
        if (bits.isNotEmpty()) {
            val bytes = bitsToBytes(bits)
            if (bytes.isNotEmpty()) {
                lastReceiveTimestamp = System.currentTimeMillis()
                onPacketBytes(bytes)
            }
        }
    }
    fun bitsToBytes(bits: List<Int>): ByteArray {
        val out = ArrayList<Byte>()
        var idx = 0
        while (idx + 10 <= bits.size) {
            if (bits[idx] == 0) {
                var b = 0
                for (j in 0 until 8) {
                    val bit = bits[idx + 1 + j]
                    b = b or (bit shl j)
                }
                out.add(b.toByte())
                idx += 10
            } else idx++
        }
        return out.toByteArray()
    }
}
