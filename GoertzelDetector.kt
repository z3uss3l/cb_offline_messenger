package com.jonas.cbproto
class GoertzelDetector(val targetFreq: Double, val sampleRate: Int, val blockSize: Int) {
    private val k = (0.5 + (blockSize * targetFreq / sampleRate)).toInt()
    private val omega = (2.0 * kotlin.math.PI * k) / blockSize
    private val coeff = 2.0 * kotlin.math.cos(omega)
    fun detect(samples: ShortArray, offset: Int = 0): Double {
        var sPrev = 0.0
        var sPrev2 = 0.0
        for (i in 0 until blockSize) {
            val x = samples[offset + i].toDouble()
            val s = x + coeff * sPrev - sPrev2
            sPrev2 = sPrev
            sPrev = s
        }
        val power = sPrev2*sPrev2 + sPrev*sPrev - coeff*sPrev*sPrev2
        return power
    }
}
