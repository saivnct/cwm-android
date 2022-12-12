package com.lgt.cwm.models.conversation.audio


data class AudioWaveFormData (
    var durationUs: Long,
    var waveForm: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioWaveFormData

        if (durationUs != other.durationUs) return false
        if (!waveForm.contentEquals(other.waveForm)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = durationUs.hashCode()
        result = 31 * result + waveForm.contentHashCode()
        return result
    }
}