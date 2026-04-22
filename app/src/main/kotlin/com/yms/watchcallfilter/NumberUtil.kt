package com.yms.watchcallfilter

import android.net.Uri

object NumberUtil {

    private val DIGIT_RE = Regex("[^\\d+]")

    fun fromHandle(handle: Uri?): String? {
        if (handle == null) return null
        if (handle.scheme != "tel") return null
        val raw = handle.schemeSpecificPart ?: return null
        return normalize(raw)
    }

    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(DIGIT_RE, "")
        return cleaned.ifEmpty { null }
    }
}
