package com.jonas.cbproto
object ModemConstants {
    const val SAMPLE_RATE = 8000
    const val TONE_DURATION_MS = 20
    const val BAUD = 50
    const val SAMPLES_PER_SYMBOL = SAMPLE_RATE * TONE_DURATION_MS / 1000
    const val FREQ_MARK = 1200.0
    const val FREQ_SPACE = 2200.0
}
